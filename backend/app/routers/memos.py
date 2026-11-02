from typing import List
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project
from app.models.memo import Memo
from app.errors import ApiError
from app.schemas.memo import MemoCreate, MemoUpdate, MemoResponse

router = APIRouter(prefix="/memos", tags=["Memos"])

@router.get("", response_model=List[MemoResponse])
def list_memos(
    project_id: int = Query(..., description="조회할 프로젝트 ID"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """특정 프로젝트의 메모 목록 조회"""
    project = db.query(Project).filter(Project.id == project_id).first()
    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")
    
    if project.user_id != current_user.id:
        raise ApiError(403, "FORBIDDEN_ACCESS", "조회 권한이 없습니다")

    memos = (
        db.query(Memo)
        .filter(Memo.project_id == project_id)
        .order_by(Memo.created_at.desc())
        .all()
    )
    return memos

@router.post("", response_model=MemoResponse, status_code=201)
def create_memo(
    req: MemoCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """메모 추가 (작성 시점의 단수 함께 기록)"""
    project = db.query(Project).filter(Project.id == req.project_id).first()
    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")
    
    if project.user_id != current_user.id:
        raise ApiError(403, "FORBIDDEN_ACCESS", "작성 권한이 없습니다")

    new_memo = Memo(
        project_id=req.project_id,
        row_number=project.current_row,  # 작성 시점 단수 자동 저장
        content=req.content
    )
    db.add(new_memo)
    db.commit()
    db.refresh(new_memo)
    return new_memo

@router.patch("/{memo_id}", response_model=MemoResponse)
def update_memo(
    memo_id: int,
    req: MemoUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """메모 내용 수정"""
    memo = db.query(Memo).filter(Memo.id == memo_id).first()
    if not memo:
        raise ApiError(404, "MEMO_NOT_FOUND", "메모를 찾을 수 없어요")
    
    project = db.query(Project).filter(Project.id == memo.project_id).first()
    if not project or project.user_id != current_user.id:
        raise ApiError(403, "FORBIDDEN_ACCESS", "수정 권한이 없습니다")

    memo.content = req.content
    db.commit()
    db.refresh(memo)
    return memo

@router.delete("/{memo_id}", status_code=204)
def delete_memo(
    memo_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """메모 삭제"""
    memo = db.query(Memo).filter(Memo.id == memo_id).first()
    if not memo:
        raise ApiError(404, "MEMO_NOT_FOUND", "메모를 찾을 수 없어요")
    
    project = db.query(Project).filter(Project.id == memo.project_id).first()
    if not project or project.user_id != current_user.id:
        raise ApiError(403, "FORBIDDEN_ACCESS", "삭제 권한이 없습니다")

    db.delete(memo)
    db.commit()
    return None