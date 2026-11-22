import hashlib
import io
import os
import uuid
from typing import Tuple

from fastapi import UploadFile
import oci
from PIL import Image, ImageOps

from app.errors import ApiError

MAX_IMAGE_BYTES = 1_000_000  # 1MB (1,000,000 바이트)
MIN_DIMENSION = 200
MAX_DIMENSION = 1024
RESIZE_MAIN = 800
RESIZE_THUMB = 320

PRIVATE_BUCKET_NAME = os.getenv("OCI_PRIVATE_BUCKET", "hanko-private")


def _get_object_storage_client() -> Tuple[oci.object_storage.ObjectStorageClient, str]:
    """OCI 인스턴스 주체(Instance Principals)로 Object Storage 클라이언트 및 네임스페이스 반환"""
    signer = oci.auth.signers.InstancePrincipalsSecurityTokenSigner()
    client = oci.object_storage.ObjectStorageClient(config={}, signer=signer)
    namespace = os.getenv("OCI_NAMESPACE") or client.get_namespace().data
    return client, namespace


async def process_square_jpeg(file: UploadFile) -> Tuple[bytes, bytes]:
    """
    업로드된 이미지를 검증(JPEG, 1MB 이하, 정사각형 200~1024px)한 뒤
    EXIF를 제거하고 800px 원본과 320px 썸네일 바이트를 반환합니다.
    """
    raw_bytes = await file.read()

    if len(raw_bytes) > MAX_IMAGE_BYTES:
        raise ApiError(
            status_code=413,
            code="PAYLOAD_TOO_LARGE",
            detail="1MB보다 큰 사진은 올릴 수 없어요",
        )

    try:
        img = Image.open(io.BytesIO(raw_bytes))
        img.verify()
        img = Image.open(io.BytesIO(raw_bytes))
    except Exception:
        raise ApiError(
            status_code=422,
            code="VALIDATION_ERROR",
            detail="사진을 확인해 주세요",
        )

    if img.format != "JPEG":
        raise ApiError(
            status_code=422,
            code="VALIDATION_ERROR",
            detail="사진을 확인해 주세요",
        )

    # EXIF 방향 태그 반영 후 메타데이터 제거 준비
    img = ImageOps.exif_transpose(img)
    width, height = img.size

    if width != height or not (MIN_DIMENSION <= width <= MAX_DIMENSION):
        raise ApiError(
            status_code=422,
            code="VALIDATION_ERROR",
            detail="사진을 확인해 주세요",
        )

    # RGB 변환 (색 프로필 유지)
    icc_profile = img.info.get("icc_profile")
    if img.mode != "RGB":
        img = img.convert("RGB")

    # 1. 원본 저장용 (800px 초과 시 축소)
    main_img = (
        img.resize((RESIZE_MAIN, RESIZE_MAIN), Image.Resampling.LANCZOS)
        if width > RESIZE_MAIN
        else img.copy()
    )
    main_buf = io.BytesIO()
    save_kwargs = {"format": "JPEG", "quality": 85, "optimize": True}
    if icc_profile:
        save_kwargs["icc_profile"] = icc_profile
    main_img.save(main_buf, **save_kwargs)

    # 2. 썸네일 저장용 (320px 초과 시 축소)
    thumb_img = (
        img.resize((RESIZE_THUMB, RESIZE_THUMB), Image.Resampling.LANCZOS)
        if width > RESIZE_THUMB
        else img.copy()
    )
    thumb_buf = io.BytesIO()
    thumb_img.save(thumb_buf, **save_kwargs)

    return main_buf.getvalue(), thumb_buf.getvalue()


def upload_private_image(prefix: str, main_bytes: bytes, thumb_bytes: bytes) -> str:
    """비공개 버킷에 원본(object_key)과 썸네일({object_key}_thumb) 업로드"""
    client, namespace = _get_object_storage_client()
    object_key = f"{prefix}/{uuid.uuid4().hex}.jpg"
    thumb_key = f"{object_key}_thumb"

    client.put_object(
        namespace_name=namespace,
        bucket_name=PRIVATE_BUCKET_NAME,
        object_name=object_key,
        put_object_body=main_bytes,
        content_type="image/jpeg",
    )
    client.put_object(
        namespace_name=namespace,
        bucket_name=PRIVATE_BUCKET_NAME,
        object_name=thumb_key,
        put_object_body=thumb_bytes,
        content_type="image/jpeg",
    )
    return object_key


def get_private_image_bytes(object_key: str, thumb: bool = False) -> Tuple[bytes, str]:
    """비공개 버킷에서 이미지 바이트 및 ETag 반환"""
    client, namespace = _get_object_storage_client()
    target_key = f"{object_key}_thumb" if thumb else object_key

    try:
        resp = client.get_object(
            namespace_name=namespace,
            bucket_name=PRIVATE_BUCKET_NAME,
            object_name=target_key,
        )
        data = resp.data.content
        etag = f'"{hashlib.sha256(data).hexdigest()}"'
        return data, etag
    except oci.exceptions.ServiceError as e:
        if e.status == 404:
            raise ApiError(
                status_code=404,
                code="IMAGE_NOT_FOUND",
                detail="이미지를 찾을 수 없어요",
            )
        raise


def delete_private_image(object_key: str | None) -> None:
    """비공개 버킷에서 원본과 썸네일 함께 삭제 (이미 없으면 무시)"""
    if not object_key:
        return

    client, namespace = _get_object_storage_client()
    for key in (object_key, f"{object_key}_thumb"):
        try:
            client.delete_object(
                namespace_name=namespace,
                bucket_name=PRIVATE_BUCKET_NAME,
                object_name=key,
            )
        except oci.exceptions.ServiceError as e:
            if e.status != 404:
                raise