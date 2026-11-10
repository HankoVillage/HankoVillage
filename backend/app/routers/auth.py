import logging
from datetime import datetime, timezone
from typing import Literal

from fastapi import APIRouter, Depends, Query, Response
from pydantic import BaseModel, Field, field_validator
from sqlalchemy import delete, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.db import get_db
from app.dependencies import get_current_user
from app.errors import ApiError
from app.models.user import User
from app.nicknames import normalize_nickname
from app.sessions import InvalidRefreshToken, end_session, rotate_session, start_session
from app.social import verify_google_token, verify_kakao_token
from app.tokens import (
    ACCESS_TOKEN_LIFETIME,
    SIGNUP_TOKEN_LIFETIME,
    InvalidSignupToken,
    create_signup_token,
    verify_signup_token,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/auth", tags=["auth"])
# Registered before any /users/{id} routes so "nickname-check" is not read as an id.
users_router = APIRouter(prefix="/users", tags=["auth"])

# A new provider needs both an entry here and a verify function in app/social.py.
Provider = Literal["kakao", "google"]
VERIFIERS = {"kakao": verify_kakao_token, "google": verify_google_token}

# Account deletion needs an ID token issued within this many seconds (a fresh sign-in).
REAUTH_MAX_AGE_SECONDS = 600


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


class WithdrawalRequest(BaseModel):
    # A fresh ID token from the same social account, to confirm it is really the owner.
    token: str = Field(min_length=1, max_length=4096)


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


def reauth_failed(detail: str) -> ApiError:
    # One code for every re-authentication failure; detail says why. 403, not 401,
    # so the app's automatic token refresh (it reacts to 401) is not triggered.
    return ApiError(403, "REAUTH_FAILED", detail)


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


@users_router.post("/me/withdrawal", status_code=204, response_class=Response)
def withdraw(
    body: WithdrawalRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # Re-authentication: a leaked access token alone must not be enough to delete the account.
    verify = VERIFIERS.get(user.provider)
    if verify is None:
        # Not a Kakao/Google member (e.g. dev_token test users): no way to re-authenticate.
        raise reauth_failed("다시 로그인해 주세요")
    try:
        provider_id = verify(body.token, max_age=REAUTH_MAX_AGE_SECONDS)
    except ApiError as exc:
        if exc.status != 401:
            raise  # e.g. 503 when Kakao/Google is unavailable
        raise reauth_failed("다시 로그인해 주세요") from exc
    if provider_id != user.provider_id:
        raise reauth_failed("지금 로그인한 계정으로 확인해 주세요")

    user_id, provider = user.id, user.provider
    # One DELETE on users; the database foreign keys do the rest:
    # sessions, device tokens, projects (and memos), patterns, yarns and likes are deleted,
    # posts and comments stay with their author set to NULL.
    db.execute(delete(User).where(User.id == user_id).execution_options(synchronize_session=False))
    db.commit()
    # Audit line without personal data (no nickname, no tokens).
    logger.info("Account deleted: user_id=%s provider=%s", user_id, provider)
    # TODO(photo storage): delete profiles/, yarns/, patterns/ files of this user after commit.
    return Response(status_code=204)