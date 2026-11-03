from fastapi import FastAPI, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text
from app.db import get_db
from app.errors import ApiError, register_error_handlers
from app.routers import auth, memos, projects

app = FastAPI(
    title="HankoVillage API Server",
    description="한코마을 앱 백엔드 API 서비스",
    version="1.0.0",
)

# 1. ApiError 및 예외 처리를 위한 글로벌 에러 핸들러 등록 (401, 404, 422 등이 500으로 터지는 문제 해결)
register_error_handlers(app)

# 2. 필수 라우터 연결 (소셜 로그인 및 닉네임 중복확인 / projects / memos)
app.include_router(auth.router)
app.include_router(auth.users_router)
app.include_router(projects.router)
app.include_router(memos.router)


@app.get("/health")
def health(db: Session = Depends(get_db)):
    try:
        db.execute(text("SELECT 1"))
    except Exception:
        raise ApiError(503, "DB_UNAVAILABLE", "잠시 후 다시 시도해 주세요")
    return {"status": "ok", "db": "ok"}