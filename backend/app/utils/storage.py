import io
import os
import uuid
from typing import Tuple

import oci
from PIL import Image, ImageOps, UnidentifiedImageError
from fastapi import UploadFile

from app.errors import ApiError

PRIVATE_BUCKET = os.getenv("OCI_PRIVATE_BUCKET", "hanko-private")
PUBLIC_BUCKET = os.getenv("OCI_PUBLIC_BUCKET", "hanko-public")
MAX_IMAGE_BYTES = 1 * 1024 * 1024  # 1MB 제한 (nginx client_max_body_size와 일치)


def _get_object_storage_client() -> Tuple[oci.object_storage.ObjectStorageClient, str]:
    """OCI 인스턴스 주체(Instance Principal) 인증으로 Object Storage 클라이언트 및 네임스페이스 반환"""
    signer = oci.auth.signers.InstancePrincipalsSecurityTokenSigner()
    client = oci.object_storage.ObjectStorageClient(config={}, signer=signer)
    namespace = client.get_namespace().data
    return client, namespace


async def process_square_jpeg(upload_file: UploadFile) -> Tuple[bytes, bytes]:
    """
    업로드된 파일을 검증하고 800x800 원본 + 300x300 썸네일 JPEG 바이트(EXIF 제거)를 반환합니다.
    """
    if upload_file.content_type not in ("image/jpeg", "image/jpg"):
        raise ApiError(422, "INVALID_IMAGE_TYPE", "JPEG 형식의 이미지만 업로드할 수 있어요")

    raw_bytes = await upload_file.read()
    if len(raw_bytes) == 0:
        raise ApiError(422, "EMPTY_FILE", "빈 파일은 업로드할 수 없어요")
    if len(raw_bytes) > MAX_IMAGE_BYTES:
        raise ApiError(413, "FILE_TOO_LARGE", "이미지 용량은 1MB 이하여야 해요")

    try:
        img = Image.open(io.BytesIO(raw_bytes))
        img.verify()
        img = Image.open(io.BytesIO(raw_bytes))
    except (UnidentifiedImageError, Exception):
        raise ApiError(422, "CORRUPTED_IMAGE", "올바른 이미지 파일이 아니에요")

    if img.format != "JPEG":
        raise ApiError(422, "INVALID_IMAGE_TYPE", "JPEG 형식의 이미지만 업로드할 수 있어요")

    img = ImageOps.exif_transpose(img).convert("RGB")
    width, height = img.size

    if width != height:
        raise ApiError(422, "INVALID_ASPECT_RATIO", "1:1 정사각형 이미지만 업로드할 수 있어요")
    if not (200 <= width <= 1024):
        raise ApiError(422, "INVALID_IMAGE_SIZE", "이미지 해상도는 200~1024px 사이여야 해요")

    # 800x800 원본 생성 (새 이미지 객체로 저장해 EXIF/GPS 메타데이터 완전 제거)
    main_img = img.resize((800, 800), Image.Resampling.LANCZOS) if width != 800 else img
    main_buf = io.BytesIO()
    main_img.save(main_buf, format="JPEG", quality=85, optimize=True)

    # 300x300 썸네일 생성
    thumb_img = img.resize((300, 300), Image.Resampling.LANCZOS)
    thumb_buf = io.BytesIO()
    thumb_img.save(thumb_buf, format="JPEG", quality=80, optimize=True)

    return main_buf.getvalue(), thumb_buf.getvalue()


def to_thumb_key(object_key: str) -> str:
    """patterns/1/abc.jpg -> patterns/1/abc_thumb.jpg"""
    base, ext = os.path.splitext(object_key)
    return f"{base}_thumb{ext}"


def upload_private_image(prefix: str, main_bytes: bytes, thumb_bytes: bytes) -> str:
    """비공개 버킷(hanko-private)에 원본과 _thumb 2개를 업로드하고 원본 object_key 반환"""
    client, namespace = _get_object_storage_client()
    object_key = f"{prefix}/{uuid.uuid4().hex}.jpg"
    thumb_key = to_thumb_key(object_key)

    client.put_object(
        namespace_name=namespace,
        bucket_name=PRIVATE_BUCKET,
        object_name=object_key,
        put_object_body=main_bytes,
        content_type="image/jpeg",
    )
    client.put_object(
        namespace_name=namespace,
        bucket_name=PRIVATE_BUCKET,
        object_name=thumb_key,
        put_object_body=thumb_bytes,
        content_type="image/jpeg",
    )
    return object_key


def get_private_image_bytes(object_key: str, thumb: bool = False) -> Tuple[bytes, str]:
    """비공개 버킷에서 이미지 바이트와 ETag 반환"""
    client, namespace = _get_object_storage_client()
    target_key = to_thumb_key(object_key) if thumb else object_key
    try:
        res = client.get_object(
            namespace_name=namespace,
            bucket_name=PRIVATE_BUCKET,
            object_name=target_key,
        )
        etag = res.headers.get("etag", "")
        return res.data.content, etag
    except oci.exceptions.ServiceError as e:
        if e.status == 404:
            raise ApiError(404, "IMAGE_NOT_FOUND", "이미지를 찾을 수 없어요")
        raise


def delete_private_image(object_key: str | None) -> None:
    """원본과 _thumb 파일 모두 삭제 (이미 없어도 무시)"""
    if not object_key:
        return
    client, namespace = _get_object_storage_client()
    for key in (object_key, to_thumb_key(object_key)):
        try:
            client.delete_object(
                namespace_name=namespace,
                bucket_name=PRIVATE_BUCKET,
                object_name=key,
            )
        except oci.exceptions.ServiceError as e:
            if e.status != 404:
                raise