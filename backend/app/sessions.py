import logging
import uuid
from datetime import datetime, timezone

from sqlalchemy import delete, select, update
from sqlalchemy.orm import Session

from app.models.refresh_token import RefreshToken
from app.tokens import (
    REFRESH_TOKEN_LIFETIME,
    create_access_token,
    hash_refresh_token,
    new_refresh_token,
)

logger = logging.getLogger(__name__)


class InvalidRefreshToken(Exception):
    pass


def _issue_refresh_token(db: Session, user_id: int, session_id: uuid.UUID, now: datetime) -> str:
    # The only place that creates a refresh token row. The caller commits.
    refresh_token = new_refresh_token()
    db.add(
        RefreshToken(
            user_id=user_id,
            session_id=session_id,
            token_hash=hash_refresh_token(refresh_token),
            # Sliding expiry: every refresh extends the login by the full lifetime.
            expires_at=now + REFRESH_TOKEN_LIFETIME,
        )
    )
    return refresh_token


def _delete_session_rows(db: Session, session_id: uuid.UUID, keep_id: int | None = None) -> None:
    # Deletes the rows of one session, except keep_id when given.
    stmt = delete(RefreshToken).where(RefreshToken.session_id == session_id)
    if keep_id is not None:
        stmt = stmt.where(RefreshToken.id != keep_id)
    db.execute(stmt.execution_options(synchronize_session=False))


def start_session(db: Session, user_id: int) -> tuple[str, str]:
    # Creates one login session. The caller commits.
    session_id = uuid.uuid4()
    refresh_token = _issue_refresh_token(db, user_id, session_id, datetime.now(timezone.utc))
    return create_access_token(user_id, session_id), refresh_token


def rotate_session(db: Session, refresh_token: str) -> tuple[str, str]:
    # Swaps a refresh token for a new pair. The caller commits, also on failure,
    # so that a session ended by reuse detection stays ended.
    token_hash = hash_refresh_token(refresh_token)
    now = datetime.now(timezone.utc)
    claimed = db.execute(
        update(RefreshToken)
        .where(
            RefreshToken.token_hash == token_hash,
            RefreshToken.used_at.is_(None),
            RefreshToken.expires_at > now,
        )
        .values(used_at=now)
        .returning(RefreshToken.id, RefreshToken.user_id, RefreshToken.session_id)
        .execution_options(synchronize_session=False)
    ).first()

    if claimed is None:
        row = db.execute(
            select(RefreshToken.session_id, RefreshToken.user_id, RefreshToken.used_at)
            .where(RefreshToken.token_hash == token_hash)
        ).first()
        if row is not None:
            session_id, user_id, used_at = row
            # Reused (possible theft, or the app lost the previous refresh response)
            # or expired: end the whole session.
            if used_at is not None:
                # Seconds between the first use and this reuse. A few seconds suggests
                # a lost response or a double request; much longer suggests theft.
                logger.warning(
                    "Refresh token reused, session ended: user_id=%s seconds_since_use=%d",
                    user_id,
                    (now - used_at).total_seconds(),
                )
            else:
                logger.info("Refresh token expired, session ended: user_id=%s", user_id)
            _delete_session_rows(db, session_id)
        raise InvalidRefreshToken()

    token_id, user_id, session_id = claimed
    # Keep only the token just used (for reuse detection) and the new one.
    _delete_session_rows(db, session_id, keep_id=token_id)
    new_token = _issue_refresh_token(db, user_id, session_id, now)
    return create_access_token(user_id, session_id), new_token


def end_session(db: Session, refresh_token: str) -> None:
    # Logout. Does nothing for an unknown token. The caller commits.
    session_id = db.scalars(
        select(RefreshToken.session_id).where(
            RefreshToken.token_hash == hash_refresh_token(refresh_token)
        )
    ).first()
    if session_id is not None:
        _delete_session_rows(db, session_id)