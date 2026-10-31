import hashlib
import secrets
import uuid
from datetime import datetime, timedelta, timezone

import jwt

from app.config import settings

ALGORITHM = "HS256"
ACCESS_TOKEN_TYP = "access+jwt"
ACCESS_TOKEN_LIFETIME = timedelta(hours=1)
REFRESH_TOKEN_BYTES = 32


class InvalidAccessToken(Exception):
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


def new_refresh_token() -> str:
    return secrets.token_urlsafe(REFRESH_TOKEN_BYTES)


def hash_refresh_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()