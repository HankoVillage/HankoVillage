from datetime import datetime, timezone
from typing import List, Optional, Literal
from fastapi import APIRouter, Depends, Query
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.errors import ApiError
from app.models.pattern import Pattern
from app.models.user import User
from app.schemas.pattern import (
    PatternCreate,
    PatternUpdate,
    PatternResponse,
    PatternTag,
    PatternStatus,
)

router = APIRouter(prefix="/patterns", tags=["Patterns"])


@router.get("", response_model=List[PatternResponse])
def list_patterns(
    q: Optional[str] = Query(None, description="제목/태그 검색어 (2글자 이상)"),
    tag: Optional[PatternTag] = Query(None, description="태그 필터"),
    status: Optional[PatternStatus] = Query(None, description="진행 상태 필터"),
    sort: Literal["NEWEST", "TITLE"] = Query("NEWEST", description="정렬 기준"),
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """내 도안 목록 조회 (검색 · 태그/상태 필터 · 최신순/제목순 정렬)"""
    query = db.query(Pattern).filter(Pattern.user_id == current_user.id)

    if q and len(q.strip()) >= 2:
        keyword = f"%{q.strip()}%"
        query = query.filter(
            or_(
                Pattern.title.ilike(keyword),
                Pattern.tag.ilike(keyword),
            )
        )

    if tag is not None:
        query = query.filter(Pattern.tag == tag)

    if status is not None:
        query = query.filter(Pattern.status == status)

    if sort == "TITLE":
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
    now = datetime.now(timezone.utc)
    new_pattern = Pattern(
        user_id=current_user.id,
        title=req.title,
        tag=req.tag,
        yarn_info=req.yarn_info,
        needle_info=req.needle_info,
        pattern_url=req.pattern_url,
        status=req.status,
        created_at=now,
        updated_at=now,
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

    # NOT NULL 컬럼에 None이 들어와 DB 500 에러가 나는 것 방지
    for non_nullable in ("title", "tag", "status"):
        if non_nullable in update_data and update_data[non_nullable] is None:
            raise ApiError(422, "VALIDATION_ERROR", f"{non_nullable} 값은 비워둘 수 없어요")

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
    """도안 삭제"""
    pattern = (
        db.query(Pattern)
        .filter(Pattern.id == pattern_id, Pattern.user_id == current_user.id)
        .first()
    )
    if not pattern:
        raise ApiError(404, "PATTERN_NOT_FOUND", "도안을 찾을 수 없어요")

    db.delete(pattern)
    db.commit()