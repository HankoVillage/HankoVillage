import hashlib
from typing import List
from fastapi import APIRouter, Depends, File, Query, Request, Response, UploadFile, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.errors import ApiError
from app.models.user import User
from app.models.yarn import Yarn
from app.schemas.yarn import YarnCreate, YarnResponse, YarnUpdate
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


# 1. 실 목록 조회 (GET /yarns?sort=&limit=&offset=)
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
    elif sort_upper in ("NEWEST", "RECENT"):
        query = query.order_by(Yarn.created_at.desc(), Yarn.id.desc())
    else:
        raise ApiError(
            status_code=422,
            code="VALIDATION_ERROR",
            detail="지원하지 않는 정렬 기준이에요",
        )

    return query.offset(offset).limit(limit).all()


# 2. 실 등록 (POST /yarns)
@router.post("", response_model=YarnResponse, status_code=status.HTTP_201_CREATED)
def create_yarn(
    req: YarnCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """새 실 등록 (라벨 사진은 PUT /yarns/{id}/image로 업로드)"""
    new_yarn = Yarn(
        user_id=current_user.id,
        name=req.name,
        color=req.color,
        label_image_key=req.label_image_key,
        purchase_date=req.purchase_date,
        purchase_site=req.purchase_site,
        memo=req.memo,
    )
    db.add(new_yarn)
    db.commit()
    db.refresh(new_yarn)
    return new_yarn


# 3. 실 상세 조회 (GET /yarns/{yarn_id})
@router.get("/{yarn_id}", response_model=YarnResponse)
def get_yarn_detail(
    yarn_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """특정 실 상세 조회"""
    return _get_user_yarn_or_404(yarn_id, current_user.id, db)


# 4. 실 정보 수정 (PATCH /yarns/{yarn_id})
@router.patch("/{yarn_id}", response_model=YarnResponse)
def update_yarn(
    yarn_id: int,
    req: YarnUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """실 정보 수정 (선택 항목에 null 전달 시 값 삭제)"""
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)
    update_data = req.model_dump(exclude_unset=True)

    # name은 NOT NULL 컬럼이므로 명시적 null 차단
    if "name" in update_data and update_data["name"] is None:
        raise ApiError(
            status_code=422,
            code="VALIDATION_ERROR",
            detail="실 이름은 비워둘 수 없어요",
        )

    for field, value in update_data.items():
        setattr(yarn, field, value)

    db.commit()
    db.refresh(yarn)
    return yarn


# 5. 실 삭제 (DELETE /yarns/{yarn_id})
@router.delete("/{yarn_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_yarn(
    yarn_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """실 삭제 (연결된 카운터의 yarn_id는 DB ON DELETE SET NULL 자동 처리)"""
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)

    if yarn.label_image_key:
        delete_private_image(yarn.label_image_key)

    db.delete(yarn)
    db.commit()
    return None


# 6. 라벨 사진 업로드 (PUT /yarns/{yarn_id}/image)
@router.put("/{yarn_id}/image", response_model=YarnResponse)
async def upload_yarn_image(
    yarn_id: int,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)

    main_jpeg, thumb_jpeg = await process_square_jpeg(file)
    new_key = upload_private_image(f"yarns/{yarn.id}", main_jpeg, thumb_jpeg)

    old_key = yarn.label_image_key
    yarn.label_image_key = new_key
    db.commit()
    db.refresh(yarn)

    if old_key and old_key != new_key:
        delete_private_image(old_key)

    return yarn


# 7. 라벨 사진 조회 (GET /yarns/{yarn_id}/image?thumb=false)
@router.get("/{yarn_id}/image")
def get_yarn_image(
    yarn_id: int,
    request: Request,
    thumb: bool = Query(False, description="true일 경우 320px 썸네일 반환"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)
    if not yarn.label_image_key:
        raise ApiError(
            status_code=404,
            code="IMAGE_NOT_FOUND",
            detail="등록된 라벨 사진이 없어요",
        )

    image_bytes, etag = get_private_image_bytes(yarn.label_image_key, thumb=thumb)
    if etag and request.headers.get("if-none-match") == etag:
        return Response(status_code=status.HTTP_304_NOT_MODIFIED)

    headers = {"Cache-Control": "private, max-age=3600"}
    if etag:
        headers["ETag"] = etag

    return Response(content=image_bytes, media_type="image/jpeg", headers=headers)


# 8. 라벨 사진 삭제 (DELETE /yarns/{yarn_id}/image)
@router.delete("/{yarn_id}/image", status_code=status.HTTP_204_NO_CONTENT)
def delete_yarn_image(
    yarn_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)

    old_key = yarn.label_image_key
    yarn.label_image_key = None
    db.commit()

    delete_private_image(old_key)
    return None