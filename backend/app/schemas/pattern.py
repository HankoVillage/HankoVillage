from datetime import datetime
from typing import Optional, Literal
from pydantic import BaseModel, Field, ConfigDict

PatternTag = Literal["clothing", "scarf", "hat", "bag", "doll", "etc"]
PatternStatus = Literal["PLANNED", "IN_PROGRESS", "COMPLETED"]


class PatternCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=50, description="도안 제목 (1~50자)")
    tag: PatternTag = Field("etc", description="카테고리 태그 코드")
    yarn_info: Optional[str] = Field(None, max_length=100, description="사용 실 정보 (100자 이하)")
    needle_info: Optional[str] = Field(None, max_length=50, description="바늘 정보 (50자 이하)")
    pattern_url: Optional[str] = Field(None, max_length=500, description="도안 링크 URL")
    status: PatternStatus = Field("PLANNED", description="진행 상태")


class PatternUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=1, max_length=50)
    tag: Optional[PatternTag] = None
    yarn_info: Optional[str] = Field(None, max_length=100)
    needle_info: Optional[str] = Field(None, max_length=50)
    pattern_url: Optional[str] = Field(None, max_length=500)
    status: Optional[PatternStatus] = None


class PatternResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    title: str
    tag: str
    yarn_info: Optional[str] = None
    needle_info: Optional[str] = None
    pattern_url: Optional[str] = None
    image_key: Optional[str] = None
    status: str
    created_at: datetime
    updated_at: datetime