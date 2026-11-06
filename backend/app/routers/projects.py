from typing import List, Optional
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project
from app.models.yarn import Yarn 
from app.errors import ApiError
from app.schemas.project import (
    ProjectCreate,
    ProjectUpdate,
    ProjectCounterUpdate,
    ProjectResponse
)

router = APIRouter(prefix="/projects", tags=["Projects"])


def validate_user_yarn(yarn_id: Optional[int], user_id: int, db: Session):
    """yarn_id가 유효하고 현재 사용자의 소유인지 검증하는 함수"""
    if yarn_id is None:
        return None
    
    yarn = db.query(Yarn).filter(
        Yarn.id == yarn_id,
        Yarn.user_id == user_id
    ).first()
    
    if not yarn:
        raise ApiError(404, "YARN_NOT_FOUND", "요청하신 실 정보를 찾을 수 없어요")
    return yarn


@router.get("", response_model=List[ProjectResponse])
def list_projects(
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """내 프로젝트 카운터 목록 조회 (페이지네이션)"""
    projects = (
        db.query(Project)
        .filter(Project.user_id == current_user.id)
        .order_by(Project.updated_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    return projects


@router.post("", response_model=ProjectResponse, status_code=201)
def create_project(
    req: ProjectCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """새 프로젝트 카운터 생성 (yarn_id 유효성 검증, needle_type 및 status 지원)"""
    if req.yarn_id is not None:
        validate_user_yarn(req.yarn_id, current_user.id, db)

    initial_row = getattr(req, "current_row", 0) or 0
    needle_type_val = getattr(req, "needle_type", "대바늘") or "대바늘"
    status_val = getattr(req, "status", None) or ("완성" if initial_row >= req.target_row else "진행")

    new_project = Project(
        user_id=current_user.id,
        name=req.name,
        needle_type=needle_type_val,
        needle=req.needle,
        yarn_id=req.yarn_id,
        current_row=initial_row,
        target_row=req.target_row,
        status=status_val,
        updated_at=datetime.now(timezone.utc)
    )
    db.add(new_project)
    db.commit()
    db.refresh(new_project)
    return new_project


@router.get("/{project_id}", response_model=ProjectResponse)
def get_project_detail(
    project_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """특정 프로젝트 상세 조회 (권한 없는 경우 404로 리소스 은닉)"""
    project = db.query(Project).filter(
        Project.id == project_id,
        Project.user_id == current_user.id
    ).first()

    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")
        
    return project


@router.patch("/{project_id}", response_model=ProjectResponse)
def update_project(
    project_id: int,
    req: ProjectUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    project = db.query(Project).filter(
        Project.id == project_id,
        Project.user_id == current_user.id
    ).first()

    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")

    update_data = req.model_dump(exclude_unset=True)

    if "name" in update_data and update_data["name"] is not None:
        project.name = update_data["name"]
    if "needle_type" in update_data and update_data["needle_type"] is not None:
        project.needle_type = update_data["needle_type"]
    if "status" in update_data and update_data["status"] is not None:
        project.status = update_data["status"]

    if "yarn_id" in update_data:
        new_yarn_id = update_data["yarn_id"]
        if new_yarn_id is not None:
            validate_user_yarn(new_yarn_id, current_user.id, db)
        project.yarn_id = new_yarn_id

    if "needle" in update_data:
        project.needle = update_data["needle"]

    if "target_row" in update_data:
        project.target_row = update_data["target_row"]

    project.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(project)
    return project

@router.patch("/{project_id}/counter", response_model=ProjectResponse)
def update_project_counter(
    project_id: int,
    req: ProjectCounterUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    단수 카운터 (+/-) 조작 시 단수 갱신
    (목표 단수 도달 시 자동 status 변경 로직 제거 완료)
    """
    project = db.query(Project).filter(
        Project.id == project_id,
        Project.user_id == current_user.id
    ).first()

    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")

    project.current_row = req.current_row
    project.updated_at = datetime.now(timezone.utc)

    db.commit()
    db.refresh(project)
    return project


@router.delete("/{project_id}", status_code=204)
def delete_project(
    project_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """프로젝트 삭제 (연관 메모 CASCADE 자동 삭제)"""
    project = db.query(Project).filter(
        Project.id == project_id,
        Project.user_id == current_user.id
    ).first()

    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")

    db.delete(project)
    db.commit()
    return None
