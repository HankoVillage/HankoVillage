from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict

class MemoCreate(BaseModel):
    content: str = Field(..., min_length=1, max_length=100, description="메모 내용 (1~100자, null 불가)")
    row_number: Optional[int] = Field(None, ge=0, description="작성 시점 단수 (0 이상, 미입력 시 프로젝트 단수 사용)")

class MemoUpdate(BaseModel):
    content: Optional[str] = Field(None, min_length=1, max_length=100, description="메모 내용 수정 (1~100자)")

class MemoResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    project_id: int
    row_number: int
    content: str
    created_at: datetime