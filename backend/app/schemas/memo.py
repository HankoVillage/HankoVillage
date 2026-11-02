from datetime import datetime
from pydantic import BaseModel, Field, ConfigDict

class MemoCreate(BaseModel):
    project_id: int = Field(..., description="연동할 프로젝트 ID")
    content: str = Field(..., min_length=1, max_length=100, description="메모 내용 (100자 이하)")

class MemoUpdate(BaseModel):
    content: str = Field(..., min_length=1, max_length=100, description="메모 내용 (100자 이하)")

class MemoResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    project_id: int
    row_number: int = Field(..., description="작성 시점의 단수")
    content: str
    created_at: datetime