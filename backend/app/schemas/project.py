from datetime import datetime
from typing import Literal, Optional
from pydantic import BaseModel, ConfigDict, Field


class ProjectCreate(BaseModel):
    """프로젝트 신규 생성 요청 스키마입니다."""

    name: str = Field(..., min_length=1, max_length=50, description="작품명")
    needle_type: Literal["knitting", "crochet", "both"] = Field(
        "knitting", description="바늘 종류"
    )
    needle: Optional[str] = Field(None, max_length=50, description="바늘 상세 정보")
    yarn_id: Optional[int] = Field(None, description="연동할 실 ID")
    current_row: int = Field(0, ge=0, description="초기 진행 단수")
    target_row: Optional[int] = Field(None, ge=1, description="목표 단수")
    status: Literal["in_progress", "done"] = Field(
        "in_progress", description="작업 진행 상태"
    )


class ProjectUpdate(BaseModel):
    """프로젝트 정보 partial 업데이트 요청 스키마입니다."""

    name: Optional[str] = Field(None, min_length=1, max_length=50, description="작품명")
    needle_type: Optional[Literal["knitting", "crochet", "both"]] = Field(
        None, description="바늘 종류"
    )
    needle: Optional[str] = Field(None, max_length=50, description="바늘 상세 정보")
    yarn_id: Optional[int] = Field(None, description="연동할 실 ID")
    target_row: Optional[int] = Field(None, ge=1, description="목표 단수")
    status: Optional[Literal["in_progress", "done"]] = Field(
        None, description="작업 진행 상태"
    )


class ProjectCounterUpdate(BaseModel):
    """카운터 단수 전용 업데이트 요청 스키마입니다."""

    current_row: int = Field(..., ge=0, description="현재 진행 단수")


class ProjectResponse(BaseModel):
    """프로젝트 상세 정보를 반환하기 위한 응답 스키마입니다."""

    # ORM 객체(SQLAlchemy 등) 데이터를 Pydantic 모델로 직접 변환하기 위해 설정
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    name: str
    needle_type: str
    needle: Optional[str] = None
    yarn_id: Optional[int] = None
    current_row: int
    target_row: Optional[int] = None
    status: str
    updated_at: datetime