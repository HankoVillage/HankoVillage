from typing import List, Optional
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project
from app.models.memo import Memo
from app.errors import ApiError
from app.schemas.memo import MemoCreate, MemoUpdate, MemoResponse

router = APIRouter(prefix="", tags=["Memos"])


@router.get("/projects/{project_id}/memos", response_model=List[MemoResponse])
def list_memos(
    project_id: int,
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """특정 프로젝트의 메모 목록 조회 (남의 프로젝트이거나 없으면 404로 리소스 은닉)"""
    project = db.query(Project).filter(
        Project.id == project_id,
        Project.user_id == current_user.id  
    ).first()

    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")

    memos = (
        db.query(Memo)
        .filter(Memo.project_id == project_id)
        .order_by(Memo.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    return memos


@router.post("/projects/{project_id}/memos", response_model=MemoResponse, status_code=201)
def create_memo(
    project_id: int,
    req: MemoCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    메모 추가 (비회원 이관 시 클라이언트의 row_number 지정 지원, 
    생략 시 해당 프로젝트의 현재 단수 current_row 사용)
    """
    project = db.query(Project).filter(
        Project.id == project_id,
        Project.user_id == current_user.id  
    ).first()

    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")
    memo_row = req.row_number if getattr(req, "row_number", None) is not None else project.current_row

    new_memo = Memo(
        project_id=project_id,
        row_number=memo_row,
        content=req.content,
        created_at=datetime.now(timezone.utc)
    )
    db.add(new_memo)
    db.commit()
    db.refresh(new_memo)
    return new_memo


@router.patch("/memos/{memo_id}", response_model=MemoResponse)
def update_memo(
    memo_id: int,
    req: MemoUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """메모 수정 (남의 메모이거나 존재하지 않는 메모면 404로 통일)"""
    memo = db.query(Memo).join(Project).filter(
        Memo.id == memo_id,
        Project.user_id == current_user.id
    ).first()

    if not memo:
        raise ApiError(404, "MEMO_NOT_FOUND", "메모를 찾을 수 없어요")

    if req.content is not None:
        memo.content = req.content

    db.commit()
    db.refresh(memo)
    return memo


@router.delete("/memos/{memo_id}", status_code=204)
def delete_memo(
    memo_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """메모 삭제 (남의 메모이거나 존재하지 않는 메모면 404로 통일)"""
    memo = db.query(Memo).join(Project).filter(
        Memo.id == memo_id,
        Project.user_id == current_user.id
    ).first()

    if not memo:
        raise ApiError(404, "MEMO_NOT_FOUND", "메모를 찾을 수 없어요")

    db.delete(memo)
    db.commit()
    return None