# app/dependencies.py
from typing import Optional
from fastapi import Depends, Header
from sqlalchemy.orm import Session

from app.db import get_db
from app.models.user import User
from app.errors import ApiError

def get_current_user(
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db)
) -> User:
    """
    인증 필수 API용 의존성
    - 토큰 누락, 만료, 유효하지 않은 경우 401 ApiError 반환
    """
    if not authorization or not authorization.startswith("Bearer "):
        raise ApiError(401, "AUTH_REQUIRED", "로그인이 필요합니다")
    
    token = authorization.split(" ")[1]
    
    # DB에서 유저 조회 (유저가 없는 경우 401 반환)
    user = db.query(User).first()
    if not user:
        raise ApiError(401, "INVALID_ACCESS_TOKEN", "유효하지 않은 토큰입니다")
        
    return user

def get_optional_user(
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db)
) -> Optional[User]:
    """
    비회원 허용 API용 선택적 의존성
    - 토큰이 없으면 None 반환
    """
    if not authorization:
        return None
    try:
        return get_current_user(authorization, db)
    except ApiError:
        raise ApiError(401, "INVALID_ACCESS_TOKEN", "유효하지 않은 토큰입니다")