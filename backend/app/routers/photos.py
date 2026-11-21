"""Photo upload, download and delete for photos only the owner may see.

PUT    /users/me/profile-image     /yarns/{yarn_id}/image     /patterns/{pattern_id}/image
GET    same paths (?size=thumb for the thumbnail)
DELETE same paths

Order for a new photo (decision 11/10 "old photo is deleted right after the DB save"):
check owner -> check and re-encode -> upload new files -> save key in DB -> delete old files.
"""

import logging
from typing import Literal

from fastapi import APIRouter, Depends, File, Header, Query, Response, UploadFile
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.errors import ApiError
from app.models.pattern import Pattern
from app.models.user import User
from app.models.yarn import Yarn
from app.photos import (
    MAX_UPLOAD_BYTES,
    InvalidPhoto,
    PhotoTooLarge,
    new_photo_key,
    photo_urls,
    process_photo,
    thumb_key,
)
from app.storage import Storage, get_private_storage

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Photos"])

JPEG = "image/jpeg"
# no-cache: the app may keep a copy but must ask before every use (If-None-Match -> 304),
# so a deleted or replaced photo is never shown from a stale copy (decision 11/22).
# private: shared caches (proxies) must not store these owner-only photos.
CACHE_CONTROL = "private, no-cache"


def photo_invalid() -> ApiError:
    return ApiError(422, "PHOTO_INVALID", "사진을 확인해 주세요")


def photo_too_large() -> ApiError:
    return ApiError(413, "PHOTO_TOO_LARGE", "1MB보다 큰 사진은 올릴 수 없어요")


def photo_not_found() -> ApiError:
    return ApiError(404, "PHOTO_NOT_FOUND", "사진이 없어요")


def photo_not_found_for_delete() -> ApiError:
    return ApiError(404, "PHOTO_NOT_FOUND", "삭제할 사진이 없어요")


def find_own_yarn(db: Session, yarn_id: int, user: User) -> Yarn:
    yarn = db.query(Yarn).filter(Yarn.id == yarn_id, Yarn.user_id == user.id).first()
    if yarn is None:
        # 404 (not 403) also for someone else's yarn: do not reveal that the id exists.
        raise ApiError(404, "YARN_NOT_FOUND", "요청하신 실 정보를 찾을 수 없어요")
    return yarn


def find_own_pattern(db: Session, pattern_id: int, user: User) -> Pattern:
    pattern = db.query(Pattern).filter(Pattern.id == pattern_id, Pattern.user_id == user.id).first()
    if pattern is None:
        raise ApiError(404, "PATTERN_NOT_FOUND", "요청하신 도안을 찾을 수 없어요")
    return pattern


def delete_photo_files(storage: Storage, key: str | None) -> None:
    """Delete a photo and its thumbnail. Call it after the DB commit; a failure only leaves an orphan file."""
    if key is None:
        return
    for k in (key, thumb_key(key)):
        try:
            storage.delete(k)
        except Exception:
            logger.warning("Could not delete stored photo %s; it is now an orphan file", k, exc_info=True)


def read_upload(file: UploadFile) -> bytes:
    # Read one byte more than allowed: if we get it, the file is too large.
    data = file.file.read(MAX_UPLOAD_BYTES + 1)
    if len(data) > MAX_UPLOAD_BYTES:
        raise photo_too_large()
    return data


def replace_photo(db: Session, storage: Storage, owner, attr: str, prefix: str, user: User, file: UploadFile, path: str) -> dict:
    data = read_upload(file)
    try:
        stored, thumbnail = process_photo(data)
    except PhotoTooLarge:
        raise photo_too_large()
    except InvalidPhoto:
        raise photo_invalid()

    key = new_photo_key(prefix, user.id)
    try:
        storage.put(key, stored, JPEG)
        storage.put(thumb_key(key), thumbnail, JPEG)
    except Exception:
        # Do not leave half an upload behind (photo without thumbnail).
        delete_photo_files(storage, key)
        raise

    old_key = getattr(owner, attr)
    setattr(owner, attr, key)
    try:
        db.commit()
    except Exception:
        db.rollback()
        # The new files are not referenced by anything: remove them, keep the old photo.
        delete_photo_files(storage, key)
        raise
    delete_photo_files(storage, old_key)
    return photo_urls(path, key)


def send_photo(storage: Storage, key: str | None, size: str | None, if_none_match: str | None) -> Response:
    if key is None:
        raise photo_not_found()
    target = thumb_key(key) if size == "thumb" else key
    etag = f'"{target.rsplit("/", 1)[-1]}"'
    headers = {"Cache-Control": CACHE_CONTROL, "ETag": etag}
    if if_none_match == etag:
        return Response(status_code=304, headers=headers)
    data = storage.get(target)
    if data is None:
        logger.warning("Photo key %s is in the DB but missing in storage", target)
        raise photo_not_found()
    return Response(content=data, media_type=JPEG, headers=headers)


def remove_photo(db: Session, storage: Storage, owner, attr: str) -> Response:
    old_key = getattr(owner, attr)
    if old_key is None:
        # Say what really happened: there was nothing to delete. The app refreshes without an error popup.
        raise photo_not_found_for_delete()
    setattr(owner, attr, None)
    db.commit()
    # After the commit the photo is gone for the API (GET answers 404), so the answer is 204
    # even if a storage delete fails; that failure is logged for the operator (decision 11/21).
    delete_photo_files(storage, old_key)
    return Response(status_code=204)


# ---- profile -------------------------------------------------------------

PROFILE_PATH = "/users/me/profile-image"


@router.put(PROFILE_PATH)
def put_profile_image(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: Storage = Depends(get_private_storage),
):
    return replace_photo(db, storage, user, "profile_image_key", "profiles", user, file, PROFILE_PATH)


@router.get(PROFILE_PATH)
def get_profile_image(
    size: Literal["thumb"] | None = Query(None),
    if_none_match: str | None = Header(None),
    user: User = Depends(get_current_user),
    storage: Storage = Depends(get_private_storage),
):
    return send_photo(storage, user.profile_image_key, size, if_none_match)


@router.delete(PROFILE_PATH, status_code=204)
def delete_profile_image(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: Storage = Depends(get_private_storage),
):
    return remove_photo(db, storage, user, "profile_image_key")


# ---- yarn label ------------------------------------------------------------


@router.put("/yarns/{yarn_id}/image")
def put_yarn_image(
    yarn_id: int,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: Storage = Depends(get_private_storage),
):
    yarn = find_own_yarn(db, yarn_id, user)
    return replace_photo(db, storage, yarn, "label_image_key", "yarns", user, file, f"/yarns/{yarn_id}/image")


@router.get("/yarns/{yarn_id}/image")
def get_yarn_image(
    yarn_id: int,
    size: Literal["thumb"] | None = Query(None),
    if_none_match: str | None = Header(None),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: Storage = Depends(get_private_storage),
):
    yarn = find_own_yarn(db, yarn_id, user)
    return send_photo(storage, yarn.label_image_key, size, if_none_match)


@router.delete("/yarns/{yarn_id}/image", status_code=204)
def delete_yarn_image(
    yarn_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: Storage = Depends(get_private_storage),
):
    yarn = find_own_yarn(db, yarn_id, user)
    return remove_photo(db, storage, yarn, "label_image_key")


# ---- finished pattern ------------------------------------------------------


@router.put("/patterns/{pattern_id}/image")
def put_pattern_image(
    pattern_id: int,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: Storage = Depends(get_private_storage),
):
    pattern = find_own_pattern(db, pattern_id, user)
    return replace_photo(db, storage, pattern, "finished_photo_key", "patterns", user, file, f"/patterns/{pattern_id}/image")


@router.get("/patterns/{pattern_id}/image")
def get_pattern_image(
    pattern_id: int,
    size: Literal["thumb"] | None = Query(None),
    if_none_match: str | None = Header(None),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: Storage = Depends(get_private_storage),
):
    pattern = find_own_pattern(db, pattern_id, user)
    return send_photo(storage, pattern.finished_photo_key, size, if_none_match)


@router.delete("/patterns/{pattern_id}/image", status_code=204)
def delete_pattern_image(
    pattern_id: int,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    storage: Storage = Depends(get_private_storage),
):
    pattern = find_own_pattern(db, pattern_id, user)
    return remove_photo(db, storage, pattern, "finished_photo_key")
