import uuid

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.db import get_db
from app.errors import ApiError
from app.models.refresh_token import RefreshToken
from app.models.user import User
from app.tokens import InvalidAccessToken, verify_access_token

bearer_scheme = HTTPBearer(auto_error=False)


def invalid_token_error() -> ApiError:
    return ApiError(
        401,
        "INVALID_ACCESS_TOKEN",
        "다시 로그인해 주세요",
        headers={"WWW-Authenticate": 'Bearer error="invalid_token"'},
    )


def find_active_user(
    db: Session, user_id: int, session_id: uuid.UUID
) -> User | None:
    # One query: the user exists AND the session still has a usable refresh token.
    # Logout, reuse detection and account deletion all remove that row.
    stmt = (
        select(User)
        .join(RefreshToken, RefreshToken.user_id == User.id)
        .where(
            User.id == user_id,
            RefreshToken.session_id == session_id,
            RefreshToken.used_at.is_(None),
            RefreshToken.expires_at > func.now(),
        )
        .limit(1)
    )
    return db.scalars(stmt).first()


def authenticate(token: str, db: Session) -> User:
    try:
        user_id, session_id = verify_access_token(token)
    except InvalidAccessToken:
        raise invalid_token_error()
    user = find_active_user(db, user_id, session_id)
    if user is None:
        raise invalid_token_error()
    return user


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None:
        raise ApiError(
            401,
            "AUTH_REQUIRED",
            "로그인이 필요해요",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return authenticate(credentials.credentials, db)


def get_optional_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User | None:
    if credentials is None:
        return None
    return authenticate(credentials.credentials, db)