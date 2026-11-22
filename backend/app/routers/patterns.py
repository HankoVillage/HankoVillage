from datetime import datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, File, Query, Request, Response, UploadFile
from sqlalchemy import case, func, literal, or_
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.errors import ApiError
from app.models.pattern import Pattern
from app.models.user import User
from app.schemas.pattern import (
    PatternCreate,
    PatternResponse,
    PatternStatus,
    PatternTag,
    PatternUpdate,
)
from app.utils.storage import (
    delete_private_image,
    get_private_image_bytes,
    process_square_jpeg,
    upload_private_image,
)

router = APIRouter(prefix="/patterns", tags=["Patterns"])

TAG_KO_TO_EN = {
    "의류": "clothing",
    "목도리": "scarf",
    "모자": "hat",
    "가방": "bag",
    "인형": "doll",
    "기타": "etc",
}


def validate_link_url(url: Optional[str]) -> Optional[str]:
    if url is None or url.strip() == "":
        return None
    cleaned = url.strip()
    if not (cleaned.startswith("http://") or cleaned.startswith("https://")):
        raise ApiError(422, "INVALID_URL", "도안 링크는 http:// 또는 https://로 시작해야 해요")
    return cleaned


@router.get("", response_model=List[PatternResponse])
def list_patterns(
    search: Optional[str] = Query(None, min_length=2, max_length=50, description="검색어 (2글자 이상)"),
    tag: Optional[PatternTag] = Query(None, description="태그 필터"),
    status: Optional[PatternStatus] = Query(None, description="진행 상태 필터"),
    sort: str = Query("NEWEST", description="정렬 기준 (NEWEST / TITLE)"),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """내 도안 목록 조회 및 검색 (pg_trgm + 관련도 순 정렬)"""
    sort_upper = sort.upper()
    if sort_upper not in ("NEWEST", "LATEST", "TITLE"):
        raise ApiError(422, "VALIDATION_ERROR", "sort 값은 NEWEST 또는 TITLE이어야 해요")

    query = db.query(Pattern).filter(Pattern.user_id == current_user.id)

    if tag is not None:
        query = query.filter(Pattern.tag == tag)

    if status is not None:
        query = query.filter(Pattern.status == status)

    if search is not None:
        kw = search.strip()
        if len(kw) < 2:
            raise ApiError(422, "VALIDATION_ERROR", "검색어는 2글자 이상 입력해 주세요")

        if len(kw) >= 3:
            title_cond = Pattern.title.ilike(f"%{kw}%")
        else:
            title_cond = literal(kw).op("<%")(Pattern.title)

        mapped_tag = TAG_KO_TO_EN.get(kw, kw.lower())

        query = query.filter(
            or_(
                title_cond,
                Pattern.tag == mapped_tag,
            )
        )

        relevance_rank = case(
            (func.lower(Pattern.title) == kw.lower(), 1),
            (Pattern.title.ilike(f"{kw}%"), 2),
            (Pattern.tag == mapped_tag, 3),
            else_=4,
        )
        if sort_upper == "TITLE":
            query = query.order_by(
                relevance_rank.asc(),
                Pattern.title.asc(),
                Pattern.created_at.desc(),
                Pattern.id.desc(),
            )
        else:
            query = query.order_by(
                relevance_rank.asc(),
                Pattern.created_at.desc(),
                Pattern.id.desc(),
            )
    else:
        if sort_upper == "TITLE":
            query = query.order_by(Pattern.title.asc(), Pattern.id.desc())
        else:
            query = query.order_by(Pattern.created_at.desc(), Pattern.id.desc())

    return query.offset(offset).limit(limit).all()


@router.post("", response_model=PatternResponse, status_code=201)
def create_pattern(
    req: PatternCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """새 도안 등록"""
    validated_url = validate_link_url(req.link_url)

    new_pattern = Pattern(
        user_id=current_user.id,
        title=req.title,
        yarn_info=req.yarn_info,
        tag=req.tag,
        status=req.status,
        link_url=validated_url,
        finished_photo_key=req.finished_photo_key,
    )
    db.add(new_pattern)
    db.commit()
    db.refresh(new_pattern)
    return new_pattern


@router.get("/{pattern_id}", response_model=PatternResponse)
def get_pattern_detail(
    pattern_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """특정 도안 상세 조회"""
    pattern = (
        db.query(Pattern)
        .filter(Pattern.id == pattern_id, Pattern.user_id == current_user.id)
        .first()
    )
    if not pattern:
        raise ApiError(404, "PATTERN_NOT_FOUND", "도안을 찾을 수 없어요")
    return pattern


@router.patch("/{pattern_id}", response_model=PatternResponse)
def update_pattern(
    pattern_id: int,
    req: PatternUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """도안 정보 및 진행 상태 수정"""
    pattern = (
        db.query(Pattern)
        .filter(Pattern.id == pattern_id, Pattern.user_id == current_user.id)
        .first()
    )
    if not pattern:
        raise ApiError(404, "PATTERN_NOT_FOUND", "도안을 찾을 수 없어요")

    update_data = req.model_dump(exclude_unset=True)

    for non_nullable in ("title", "tag", "status"):
        if non_nullable in update_data and update_data[non_nullable] is None:
            raise ApiError(422, "VALIDATION_ERROR", f"{non_nullable} 값은 비워둘 수 없어요")

    if "link_url" in update_data:
        update_data["link_url"] = validate_link_url(update_data["link_url"])

    for field, value in update_data.items():
        setattr(pattern, field, value)

    pattern.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(pattern)
    return pattern


@router.delete("/{pattern_id}", status_code=204)
def delete_pattern(
    pattern_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """도안 삭제 (첨부된 완성작 사진도 함께 정리)"""
    pattern = (
        db.query(Pattern)
        .filter(Pattern.id == pattern_id, Pattern.user_id == current_user.id)
        .first()
    )
    if not pattern:
        raise ApiError(404, "PATTERN_NOT_FOUND", "도안을 찾을 수 없어요")

    old_key = pattern.finished_photo_key
    db.delete(pattern)
    db.commit()
    if old_key:
        delete_private_image(old_key)


@router.put("/{pattern_id}/image", response_model=PatternResponse)
async def upload_pattern_image(
    pattern_id: int,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """도안 완성작 사진 등록 및 교체 (800x800 원본 + 300x300 썸네일)"""
    pattern = (
        db.query(Pattern)
        .filter(Pattern.id == pattern_id, Pattern.user_id == current_user.id)
        .first()
    )
    if not pattern:
        raise ApiError(404, "PATTERN_NOT_FOUND", "도안을 찾을 수 없어요")

    main_bytes, thumb_bytes = await process_square_jpeg(file)
    new_key = upload_private_image(f"patterns/{pattern.id}", main_bytes, thumb_bytes)

    old_key = pattern.finished_photo_key
    pattern.finished_photo_key = new_key
    pattern.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(pattern)

    if old_key and old_key != new_key:
        delete_private_image(old_key)

    return pattern


@router.get("/{pattern_id}/image")
def download_pattern_image(
    pattern_id: int,
    request: Request,
    thumb: bool = Query(False, description="true면 300px 썸네일 반환"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """도안 완성작 비공개 사진 조회 (본인만 접근 가능)"""
    pattern = (
        db.query(Pattern)
        .filter(Pattern.id == pattern_id, Pattern.user_id == current_user.id)
        .first()
    )
    if not pattern or not pattern.finished_photo_key:
        raise ApiError(404, "IMAGE_NOT_FOUND", "이미지를 찾을 수 없어요")

    image_bytes, etag = get_private_image_bytes(pattern.finished_photo_key, thumb=thumb)
    if etag and request.headers.get("if-none-match") == etag:
        return Response(status_code=304)

    headers = {"Cache-Control": "private, max-age=3600"}
    if etag:
        headers["ETag"] = etag

    return Response(content=image_bytes, media_type="image/jpeg", headers=headers)


@router.delete("/{pattern_id}/image", status_code=204)
def remove_pattern_image(
    pattern_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """도안 완성작 사진만 삭제"""
    pattern = (
        db.query(Pattern)
        .filter(Pattern.id == pattern_id, Pattern.user_id == current_user.id)
        .first()
    )
    if not pattern:
        raise ApiError(404, "PATTERN_NOT_FOUND", "도안을 찾을 수 없어요")

    old_key = pattern.finished_photo_key
    pattern.finished_photo_key = None
    pattern.updated_at = datetime.now(timezone.utc)
    db.commit()

    if old_key:
        delete_private_image(old_key)