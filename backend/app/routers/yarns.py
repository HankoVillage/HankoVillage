import hashlib
from typing import List, Optional
from fastapi import APIRouter, Depends, File, Query, Request, Response, UploadFile, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.errors import ApiError
from app.models.user import User
from app.models.yarn import Yarn
from app.schemas.yarn import YarnCreate, YarnImageResponse, YarnResponse, YarnUpdate
from app.utils.storage import (
    delete_private_image,
    get_private_image_bytes,
    process_square_jpeg,
    upload_private_image,
)

router = APIRouter(prefix="/yarns", tags=["Yarns"])


def _get_user_yarn_or_404(yarn_id: int, user_id: int, db: Session) -> Yarn:
    yarn = (
        db.query(Yarn)
        .filter(Yarn.id == yarn_id, Yarn.user_id == user_id)
        .first()
    )
    if not yarn:
        raise ApiError(
            status_code=404,
            code="YARN_NOT_FOUND",
            detail="요청하신 실 정보를 찾을 수 없어요",
        )
    return yarn


def _normalize_optional_text(value: Optional[str]) -> Optional[str]:
    if value is None:
        return None
    cleaned = value.strip()
    return cleaned if cleaned else None


@router.get("", response_model=List[YarnResponse])
def list_yarns(
    sort: str = Query("NEWEST", description="정렬 기준 (NEWEST: 최신순, NAME: 이름순)"),
    limit: int = Query(20, ge=1, le=100, description="조회 개수"),
    offset: int = Query(0, ge=0, description="건너뛸 개수"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """내 실 보관소 목록 조회"""
    query = db.query(Yarn).filter(Yarn.user_id == current_user.id)

    sort_upper = sort.strip().upper()
    if sort_upper in ("NAME", "TITLE"):
        query = query.order_by(Yarn.name.asc(), Yarn.id.desc())
    elif sort_upper in ("NEWEST", "RECENT", "LATEST"):
        query = query.order_by(Yarn.created_at.desc(), Yarn.id.desc())
    else:
        raise ApiError(
            status_code=422,
            code="VALIDATION_ERROR",
            detail="지원하지 않는 정렬 기준이에요",
        )

    return query.offset(offset).limit(limit).all()


@router.post("", response_model=YarnResponse, status_code=status.HTTP_201_CREATED)
def create_yarn(
    req: YarnCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """새 실 등록 (라벨 사진은 PUT /yarns/{id}/image로 업로드)"""
    cleaned_name = req.name.strip()
    if not cleaned_name:
        raise ApiError(
            status_code=422,
            code="VALIDATION_ERROR",
            detail="실 이름은 비워둘 수 없어요",
        )

    new_yarn = Yarn(
        user_id=current_user.id,
        name=cleaned_name,
        color=_normalize_optional_text(req.color),
        purchase_date=req.purchase_date,
        purchase_site=_normalize_optional_text(req.purchase_site),
        memo=_normalize_optional_text(req.memo),
    )
    db.add(new_yarn)
    db.commit()
    db.refresh(new_yarn)
    return new_yarn


@router.get("/{yarn_id}", response_model=YarnResponse)
def get_yarn_detail(
    yarn_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """특정 실 상세 조회"""
    return _get_user_yarn_or_404(yarn_id, current_user.id, db)


@router.patch("/{yarn_id}", response_model=YarnResponse)
def update_yarn(
    yarn_id: int,
    req: YarnUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """실 정보 수정"""
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)
    update_data = req.model_dump(exclude_unset=True)

    if "name" in update_data:
        if update_data["name"] is None or not update_data["name"].strip():
            raise ApiError(
                status_code=422,
                code="VALIDATION_ERROR",
                detail="실 이름은 비워둘 수 없어요",
            )
        update_data["name"] = update_data["name"].strip()

    for nullable_text_field in ("color", "purchase_site", "memo"):
        if nullable_text_field in update_data:
            update_data[nullable_text_field] = _normalize_optional_text(update_data[nullable_text_field])

    for field, value in update_data.items():
        setattr(yarn, field, value)

    db.commit()
    db.refresh(yarn)
    return yarn


@router.delete("/{yarn_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_yarn(
    yarn_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """실 삭제 및 연결된 라벨 사진 정리"""
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)

    old_key = yarn.label_image_key
    db.delete(yarn)
    db.commit()
    delete_private_image(old_key)
    return None


@router.put("/{yarn_id}/image", response_model=YarnImageResponse)
async def upload_yarn_image(
    yarn_id: int,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """실 라벨 사진 등록 및 교체"""
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)

    main_jpeg, thumb_jpeg = await process_square_jpeg(file)
    new_key = upload_private_image(f"yarns/{yarn.id}", main_jpeg, thumb_jpeg)

    old_key = yarn.label_image_key
    yarn.label_image_key = new_key
    db.commit()
    db.refresh(yarn)

    if old_key and old_key != new_key:
        delete_private_image(old_key)

    return YarnImageResponse(
        image_url=f"/yarns/{yarn.id}/image",
        thumbnail_url=f"/yarns/{yarn.id}/image?thumb=true",
    )


@router.get("/{yarn_id}/image")
def get_yarn_image(
    yarn_id: int,
    request: Request,
    thumb: bool = Query(False, description="true일 경우 썸네일 반환"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """실 라벨 사진 비공개 조회"""
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)
    if not yarn.label_image_key:
        raise ApiError(
            status_code=404,
            code="IMAGE_NOT_FOUND",
            detail="이미지를 찾을 수 없어요",
        )

    image_bytes, etag = get_private_image_bytes(yarn.label_image_key, thumb=thumb)
    if etag and request.headers.get("if-none-match") == etag:
        return Response(status_code=status.HTTP_304_NOT_MODIFIED)

    headers = {"Cache-Control": "private, max-age=3600"}
    if etag:
        headers["ETag"] = etag

    return Response(content=image_bytes, media_type="image/jpeg", headers=headers)


@router.delete("/{yarn_id}/image", status_code=status.HTTP_204_NO_CONTENT)
def delete_yarn_image(
    yarn_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """실 라벨 사진만 삭제"""
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)

    old_key = yarn.label_image_key
    yarn.label_image_key = None
    db.commit()
    delete_private_image(old_key)
    return None