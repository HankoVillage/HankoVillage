from datetime import datetime, timezone
from typing import Literal

from fastapi import APIRouter, Depends, Query, Response
from pydantic import BaseModel, Field, field_validator
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.db import get_db
from app.errors import ApiError
from app.models.user import User
from app.nicknames import normalize_nickname
from app.sessions import InvalidRefreshToken, end_session, rotate_session, start_session
from app.social import verify_kakao_token
from app.tokens import (
    ACCESS_TOKEN_LIFETIME,
    SIGNUP_TOKEN_LIFETIME,
    InvalidSignupToken,
    create_signup_token,
    verify_signup_token,
)

router = APIRouter(prefix="/auth", tags=["auth"])
# Registered before any /users/{id} routes so "nickname-check" is not read as an id.
users_router = APIRouter(prefix="/users", tags=["auth"])

# Add "google" here together with its verify function.
Provider = Literal["kakao"]
VERIFIERS = {"kakao": verify_kakao_token}


class SocialLoginRequest(BaseModel):
    token: str = Field(min_length=1, max_length=4096)


class LoginUser(BaseModel):
    id: int
    nickname: str


class LoginResponse(BaseModel):
    is_new_user: Literal[False] = False
    access_token: str
    refresh_token: str
    token_type: Literal["bearer"] = "bearer"
    expires_in: int
    user: LoginUser


class SignupRequiredResponse(BaseModel):
    is_new_user: Literal[True] = True
    signup_token: str
    signup_expires_in: int


class SignupRequest(BaseModel):
    signup_token: str = Field(min_length=1, max_length=2048)
    nickname: str
    terms_agreed: Literal[True]

    @field_validator("nickname")
    @classmethod
    def check_nickname(cls, value: str) -> str:
        nickname = normalize_nickname(value)
        if nickname is None:
            raise ValueError("invalid nickname format")
        return nickname


class NicknameCheckResponse(BaseModel):
    available: bool
    reason: Literal["DUPLICATE", "INVALID_FORMAT"] | None


class RefreshRequest(BaseModel):
    refresh_token: str = Field(min_length=1, max_length=256)


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: Literal["bearer"] = "bearer"
    expires_in: int


def invalid_signup_token() -> ApiError:
    return ApiError(401, "INVALID_SIGNUP_TOKEN", "다시 로그인해 주세요")


def already_registered() -> ApiError:
    return ApiError(409, "ALREADY_REGISTERED", "이미 가입된 계정이에요. 다시 로그인해 주세요")


def nickname_duplicate() -> ApiError:
    return ApiError(409, "NICKNAME_DUPLICATE", "이미 사용 중인 닉네임이에요")


def invalid_refresh_token() -> ApiError:
    return ApiError(401, "INVALID_REFRESH_TOKEN", "다시 로그인해 주세요")


def find_user(db: Session, provider: str, provider_id: str) -> User | None:
    return db.scalars(
        select(User).where(User.provider == provider, User.provider_id == provider_id)
    ).first()


def nickname_taken(db: Session, nickname: str) -> bool:
    return db.scalars(select(User.id).where(User.nickname == nickname)).first() is not None


def login_response(db: Session, user: User) -> LoginResponse:
    # Starts a session and commits it.
    access_token, refresh_token = start_session(db, user.id)
    db.commit()
    return LoginResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=int(ACCESS_TOKEN_LIFETIME.total_seconds()),
        user=LoginUser(id=user.id, nickname=user.nickname),
    )


@router.post("/social/{provider}", response_model=LoginResponse | SignupRequiredResponse)
def social_login(provider: Provider, body: SocialLoginRequest, db: Session = Depends(get_db)):
    provider_id = VERIFIERS[provider](body.token)

    user = find_user(db, provider, provider_id)
    if user is None:
        return SignupRequiredResponse(
            signup_token=create_signup_token(provider, provider_id),
            signup_expires_in=int(SIGNUP_TOKEN_LIFETIME.total_seconds()),
        )
    return login_response(db, user)


@router.post("/signup", response_model=LoginResponse, status_code=201)
def signup(body: SignupRequest, db: Session = Depends(get_db)):
    try:
        provider, provider_id = verify_signup_token(body.signup_token)
    except InvalidSignupToken:
        raise invalid_signup_token()

    if find_user(db, provider, provider_id) is not None:
        raise already_registered()
    if nickname_taken(db, body.nickname):
        raise nickname_duplicate()

    user = User(
        nickname=body.nickname,
        provider=provider,
        provider_id=provider_id,
        terms_agreed_at=datetime.now(timezone.utc),
    )
    db.add(user)
    try:
        db.flush()
    except IntegrityError:
        # Someone else won the race between our checks and the insert.
        db.rollback()
        if find_user(db, provider, provider_id) is not None:
            raise already_registered()
        raise nickname_duplicate()
    return login_response(db, user)


@router.post("/refresh", response_model=TokenResponse)
def refresh(body: RefreshRequest, db: Session = Depends(get_db)):
    try:
        access_token, refresh_token = rotate_session(db, body.refresh_token)
    except InvalidRefreshToken:
        # Save the session deletion from reuse detection before answering 401.
        db.commit()
        raise invalid_refresh_token()
    db.commit()
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=int(ACCESS_TOKEN_LIFETIME.total_seconds()),
    )


@router.post("/logout", status_code=204, response_class=Response)
def logout(body: RefreshRequest, db: Session = Depends(get_db)):
    # Always 204, so the app can clear its tokens whatever the server state is.
    end_session(db, body.refresh_token)
    db.commit()
    return Response(status_code=204)


@users_router.get("/nickname-check", response_model=NicknameCheckResponse)
def nickname_check(nickname: str = Query(), db: Session = Depends(get_db)):
    normalized = normalize_nickname(nickname)
    if normalized is None:
        return NicknameCheckResponse(available=False, reason="INVALID_FORMAT")
    if nickname_taken(db, normalized):
        return NicknameCheckResponse(available=False, reason="DUPLICATE")
    return NicknameCheckResponse(available=True, reason=None)