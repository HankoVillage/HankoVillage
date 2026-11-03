import hashlib
import secrets
import uuid
from datetime import datetime, timedelta, timezone

import jwt

from app.config import settings

ALGORITHM = "HS256"
ACCESS_TOKEN_TYP = "access+jwt"
ACCESS_TOKEN_LIFETIME = timedelta(hours=1)
REFRESH_TOKEN_LIFETIME = timedelta(days=90)
REFRESH_TOKEN_BYTES = 32
SIGNUP_TOKEN_TYP = "signup+jwt"
SIGNUP_TOKEN_LIFETIME = timedelta(minutes=10)


class InvalidAccessToken(Exception):
    pass


class InvalidSignupToken(Exception):
    pass


def create_access_token(user_id: int, session_id: uuid.UUID) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": str(user_id),
        "sid": str(session_id),
        "iat": now,
        "exp": now + ACCESS_TOKEN_LIFETIME,
    }
    return jwt.encode(
        payload,
        settings.jwt_access_secret.get_secret_value(),
        algorithm=ALGORITHM,
        headers={"typ": ACCESS_TOKEN_TYP},
    )


def verify_access_token(token: str) -> tuple[int, uuid.UUID]:
    try:
        decoded = jwt.decode_complete(
            token,
            settings.jwt_access_secret.get_secret_value(),
            algorithms=[ALGORITHM],
            options={"require": ["sub", "sid", "iat", "exp"]},
        )
        if decoded["header"].get("typ") != ACCESS_TOKEN_TYP:
            raise InvalidAccessToken()
        payload = decoded["payload"]
        return int(payload["sub"]), uuid.UUID(payload["sid"])
    except (jwt.InvalidTokenError, ValueError) as exc:
        raise InvalidAccessToken() from exc


def create_signup_token(provider: str, provider_id: str) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "provider": provider,
        "provider_id": provider_id,
        "iat": now,
        "exp": now + SIGNUP_TOKEN_LIFETIME,
    }
    return jwt.encode(
        payload,
        settings.jwt_signup_secret.get_secret_value(),
        algorithm=ALGORITHM,
        headers={"typ": SIGNUP_TOKEN_TYP},
    )


def verify_signup_token(token: str) -> tuple[str, str]:
    try:
        decoded = jwt.decode_complete(
            token,
            settings.jwt_signup_secret.get_secret_value(),
            algorithms=[ALGORITHM],
            options={"require": ["provider", "provider_id", "iat", "exp"]},
        )
    except jwt.InvalidTokenError as exc:
        raise InvalidSignupToken() from exc
    if decoded["header"].get("typ") != SIGNUP_TOKEN_TYP:
        raise InvalidSignupToken()
    payload = decoded["payload"]
    provider = payload["provider"]
    provider_id = payload["provider_id"]
    if not isinstance(provider, str) or not isinstance(provider_id, str) or not provider_id:
        raise InvalidSignupToken()
    return provider, provider_id


def new_refresh_token() -> str:
    return secrets.token_urlsafe(REFRESH_TOKEN_BYTES)


def hash_refresh_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()