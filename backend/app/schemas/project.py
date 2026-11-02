from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict

class ProjectCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=50, description="작품명 (50자 이내)")
    yarn_id: Optional[int] = Field(None, description="실 보관소 실 ID (선택)")
    needle: Optional[str] = Field(None, max_length=50, description="바늘 정보 (50자 이내)")
    target_row: Optional[int] = Field(None, ge=1, description="목표 단수 (1 이상, 선택)")

class ProjectCounterUpdate(BaseModel):
    current_row: int = Field(..., ge=0, description="현재 진행 단수 (0 이상)")

class ProjectUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=50)
    yarn_id: Optional[int] = None
    needle: Optional[str] = Field(None, max_length=50)
    target_row: Optional[int] = Field(None, ge=1)

class ProjectResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    name: str
    yarn_id: Optional[int] = None
    needle: Optional[str] = None
    current_row: int
    target_row: Optional[int] = None
    updated_at: datetime