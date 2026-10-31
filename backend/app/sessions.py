import uuid
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.models.refresh_token import RefreshToken
from app.tokens import (
    REFRESH_TOKEN_LIFETIME,
    create_access_token,
    hash_refresh_token,
    new_refresh_token,
)


def start_session(db: Session, user_id: int) -> tuple[str, str]:
    # Creates one login session. The caller commits.
    session_id = uuid.uuid4()
    refresh_token = new_refresh_token()
    db.add(
        RefreshToken(
            user_id=user_id,
            session_id=session_id,
            token_hash=hash_refresh_token(refresh_token),
            expires_at=datetime.now(timezone.utc) + REFRESH_TOKEN_LIFETIME,
        )
    )
    return create_access_token(user_id, session_id), refresh_token