from pydantic import BaseModel, ConfigDict, Field
from typing import Optional
from datetime import datetime


# 메모 생성 요청 스키마 (비회원 이관용 row_number 지원 및 Field 검증)
class MemoCreate(BaseModel):
    content: str
    row_number: Optional[int] = Field(None, ge=0, 
                                      description="작성 시점 단수 (비회원 이관용, 미입력 시 프로젝트 단수 사용)")


# 메모 수정 요청 스키마
class MemoUpdate(BaseModel):
    content: Optional[str] = None


# 메모 응답 스키마
class MemoResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    project_id: int
    row_number: int
    content: str
    created_at: datetime