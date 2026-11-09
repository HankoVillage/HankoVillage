"""Issue test access tokens without social login (development only).

Run inside the api container. This is not an API endpoint.

  docker compose exec -T api python -m scripts.dev_token issue --nickname tester1
  docker compose exec -T api python -m scripts.dev_token issue --nickname tester1 --show-refresh
  echo "$TOKEN" | docker compose exec -T api python -m scripts.dev_token check
  docker compose exec -T api python -m scripts.dev_token cleanup
"""
import argparse
import sys
from datetime import datetime, timezone

from sqlalchemy import delete, select
from sqlalchemy.exc import IntegrityError

from app.db import SessionLocal
from app.dependencies import authenticate
from app.errors import ApiError
from app.models.user import User
from app.sessions import start_session

TEST_PROVIDER = "test"
NICKNAME_MAX = 10


def issue(nickname: str, show_refresh: bool) -> int:
    if not 1 <= len(nickname) <= NICKNAME_MAX:
        print(f"nickname must be 1-{NICKNAME_MAX} characters", file=sys.stderr)
        return 1
    with SessionLocal() as db:
        user = db.scalars(
            select(User).where(
                User.provider == TEST_PROVIDER, User.provider_id == nickname
            )
        ).first()
        if user is None:
            user = User(
                nickname=nickname,
                provider=TEST_PROVIDER,
                provider_id=nickname,
                terms_agreed_at=datetime.now(timezone.utc),
            )
            db.add(user)
            try:
                db.flush()
            except IntegrityError:
                print("nickname is already used by a real user", file=sys.stderr)
                return 1
        user_id = user.id
        access_token, refresh_token = start_session(db, user_id)
        db.commit()
    print(
        f"test user id={user_id} nickname={nickname}, token valid for 1 hour",
        file=sys.stderr,
    )
    print(access_token)
    if show_refresh:
        # Second line, only when asked, so existing one-line usage keeps working.
        print(refresh_token)
    return 0


def check() -> int:
    token = sys.stdin.readline().strip()
    with SessionLocal() as db:
        try:
            user = authenticate(token, db)
        except ApiError as exc:
            print(f"rejected: {exc.code}")
            return 1
        print(f"ok: user id={user.id} nickname={user.nickname}")
    return 0


def cleanup() -> int:
    with SessionLocal() as db:
        result = db.execute(delete(User).where(User.provider == TEST_PROVIDER))
        db.commit()
    print(f"deleted {result.rowcount} test user(s) and their sessions")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Development test tokens")
    sub = parser.add_subparsers(dest="command", required=True)
    issue_parser = sub.add_parser("issue", help="create a test user and session")
    issue_parser.add_argument("--nickname", default="tester1")
    issue_parser.add_argument(
        "--show-refresh",
        action="store_true",
        help="also print the refresh token on a second line",
    )
    sub.add_parser("check", help="read a token from stdin and verify it")
    sub.add_parser("cleanup", help="delete all test users and their sessions")
    args = parser.parse_args()
    if args.command == "issue":
        return issue(args.nickname, args.show_refresh)
    if args.command == "check":
        return check()
    return cleanup()


if __name__ == "__main__":
    sys.exit(main())