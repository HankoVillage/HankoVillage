from typing import List, Optional
from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.errors import ApiError
from app.models.user import User
from app.models.yarn import Yarn
from app.routers.photos import delete_photo_files
from app.schemas.yarn import YarnCreate, YarnResponse, YarnUpdate
from app.storage import get_private_storage
from app.routers.photos import delete_photo_files

router = APIRouter(prefix="/yarns", tags=["Yarns"])


def _get_user_yarn_or_404(yarn_id: int, user_id: int, db: Session) -> Yarn:
    yarn = (
        db.query(Yarn)
        .filter(Yarn.id == yarn_id, Yarn.user_id == user_id)
        .first()
    )
    if not yarn:
        raise ApiError(404, "YARN_NOT_FOUND", "요청하신 실 정보를 찾을 수 없어요")
    return yarn


def _normalize_optional_text(value: Optional[str]) -> Optional[str]:
    if value is None:
        return None
    cleaned = value.strip()
    return cleaned if cleaned else None


@router.get("", response_model=List[YarnResponse])
def list_yarns(
    sort: str = Query("NEWEST"),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    query = db.query(Yarn).filter(Yarn.user_id == current_user.id)

    sort_upper = sort.strip().upper()
    if sort_upper in ("NAME", "TITLE"):
        query = query.order_by(Yarn.name.asc(), Yarn.id.desc())
    elif sort_upper in ("NEWEST", "RECENT", "LATEST"):
        query = query.order_by(Yarn.created_at.desc(), Yarn.id.desc())
    else:
        raise ApiError(422, "VALIDATION_ERROR", "지원하지 않는 정렬 기준이에요")

    return query.offset(offset).limit(limit).all()


@router.post("", response_model=YarnResponse, status_code=status.HTTP_201_CREATED)
def create_yarn(
    req: YarnCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    cleaned_name = req.name.strip()
    if not cleaned_name:
        raise ApiError(422, "VALIDATION_ERROR", "실 이름은 비워둘 수 없어요")

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
    return _get_user_yarn_or_404(yarn_id, current_user.id, db)


@router.patch("/{yarn_id}", response_model=YarnResponse)
def update_yarn(
    yarn_id: int,
    req: YarnUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)
    update_data = req.model_dump(exclude_unset=True)

    if "name" in update_data:
        if update_data["name"] is None or not update_data["name"].strip():
            raise ApiError(422, "VALIDATION_ERROR", "실 이름은 비워둘 수 없어요")
        update_data["name"] = update_data["name"].strip()

    for field in ("color", "purchase_site", "memo"):
        if field in update_data:
            update_data[field] = _normalize_optional_text(update_data[field])

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
    yarn = _get_user_yarn_or_404(yarn_id, current_user.id, db)
    old_key = yarn.label_image_key

    db.delete(yarn)
    db.commit()

    if old_key:
        storage = get_private_storage()
        delete_photo_files(storage, old_key)

    return None 