from typing import List
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from datetime import datetime

from app.db import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project
from app.errors import ApiError
from app.schemas.project import (
    ProjectCreate,
    ProjectUpdate,
    ProjectCounterUpdate,
    ProjectResponse
)

router = APIRouter(prefix="/projects", tags=["Projects"])

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
    """새 프로젝트 카운터 생성"""
    new_project = Project(
        user_id=current_user.id,
        name=req.name,
        yarn_id=req.yarn_id,
        needle=req.needle,
        current_row=0,
        target_row=req.target_row,
        updated_at=datetime.utcnow()
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
    """특정 프로젝트 상세 조회"""
    project = db.query(Project).filter(Project.id == project_id).first()
    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")
    
    if project.user_id != current_user.id:
        raise ApiError(403, "FORBIDDEN_ACCESS", "접근 권한이 없는 프로젝트입니다")
        
    return project

@router.patch("/{project_id}/counter", response_model=ProjectResponse)
def update_counter(
    project_id: int,
    req: ProjectCounterUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """단수 갱신 (Last-Write-Wins 방식)"""
    project = db.query(Project).filter(Project.id == project_id).first()
    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")
    
    if project.user_id != current_user.id:
        raise ApiError(403, "FORBIDDEN_ACCESS", "수정 권한이 없습니다")

    project.current_row = req.current_row
    project.updated_at = datetime.utcnow()
    
    db.commit()
    db.refresh(project)
    return project

@router.patch("/{project_id}", response_model=ProjectResponse)
def update_project(
    project_id: int,
    req: ProjectUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """프로젝트 정보(이름, 실, 바늘, 목표 단수) 수정"""
    project = db.query(Project).filter(Project.id == project_id).first()
    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")
    
    if project.user_id != current_user.id:
        raise ApiError(403, "FORBIDDEN_ACCESS", "수정 권한이 없습니다")

    if req.name is not None:
        project.name = req.name
    if req.yarn_id is not None:
        project.yarn_id = req.yarn_id
    if req.needle is not None:
        project.needle = req.needle
    if req.target_row is not None:
        project.target_row = req.target_row

    project.updated_at = datetime.utcnow()
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
    project = db.query(Project).filter(Project.id == project_id).first()
    if not project:
        raise ApiError(404, "PROJECT_NOT_FOUND", "프로젝트를 찾을 수 없어요")
    
    if project.user_id != current_user.id:
        raise ApiError(403, "FORBIDDEN_ACCESS", "삭제 권한이 없습니다")

    db.delete(project)
    db.commit()
    return None