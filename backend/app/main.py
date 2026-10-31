from fastapi import FastAPI, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text
from app.db import get_db
from app.errors import ApiError, register_error_handlers

app = FastAPI()
register_error_handlers(app)


@app.get("/health")
def health(db: Session = Depends(get_db)):
    try:
        db.execute(text("SELECT 1"))
    except Exception:
        raise ApiError(503, "DB_UNAVAILABLE", "잠시 후 다시 시도해 주세요")
    return {"status": "ok", "db": "ok"}