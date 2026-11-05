from datetime import datetime
from typing import Optional, Literal
from pydantic import BaseModel, Field, ConfigDict


class ProjectCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=50, description="작품명 (1~50자, null 불가)")
    needle_type: Literal["knitting", "crochet", "both"] = Field("knitting", description="바늘 종류 ('knitting', 'crochet', 'both')")
    needle: Optional[str] = Field(None, max_length=50, description="바늘 상세 정보 (50자 이하)")
    yarn_id: Optional[int] = Field(None, description="연동할 실 ID")
    current_row: int = Field(0, ge=0, description="초기 진행 단수 (0 이상)")
    target_row: Optional[int] = Field(None, ge=1, description="목표 단수 (선택 사항, 1 이상)")
    status: Literal["진행", "완성"] = Field("진행", description="상태 ('진행' 또는 '완성')")


class ProjectUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=50, description="작품명 수정 (1~50자)")
    needle_type: Optional[Literal["knitting", "crochet", "both"]] = Field(None, description="바늘 종류 수정")
    needle: Optional[str] = Field(None, max_length=50, description="바늘 정보 수정 (50자 이하)")
    yarn_id: Optional[int] = Field(None, description="실 ID 수정 (null 전달 시 선택 해제)")
    target_row: Optional[int] = Field(None, ge=1, description="목표 단수 수정 (null 전달 시 삭제)")
    status: Optional[Literal["진행", "완성"]] = Field(None, description="상태 수정")


class ProjectCounterUpdate(BaseModel):
    current_row: int = Field(..., ge=0, description="현재 진행 단수 (0 이상, null 불가)")


class ProjectResponse(BaseModel):
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