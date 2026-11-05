from pydantic import BaseModel, ConfigDict, Field
from typing import Optional
from datetime import datetime


# 프로젝트 생성 요청 스키마 (current_row, needle_type, status 포함)
class ProjectCreate(BaseModel):
    name: str
    needle_type: Optional[str] = "대바늘"
    needle: Optional[str] = None
    yarn_id: Optional[int] = None
    current_row: int = Field(0, ge=0, description="초기 진행 단수 (비회원 이관 시 전송)")
    target_row: int = Field(100, ge=1, description="목표 단수 (최소 1단 이상)")
    status: Optional[str] = "진행"


# 프로젝트 수정 요청 스키마 (PATCH /projects/{id})
class ProjectUpdate(BaseModel):
    name: Optional[str] = None
    needle_type: Optional[str] = None
    needle: Optional[str] = None
    yarn_id: Optional[int] = None
    target_row: Optional[int] = Field(None, ge=1)
    status: Optional[str] = None


# 카운터 단수 갱신 스키마 (PATCH /projects/{id}/counter)
class ProjectCounterUpdate(BaseModel):
    current_row: int = Field(..., ge=0)


# 프로젝트 응답 스키마
class ProjectResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    name: str
    needle_type: str
    needle: Optional[str] = None
    yarn_id: Optional[int] = None
    current_row: int
    target_row: int
    status: str
    updated_at: datetime