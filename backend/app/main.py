from fastapi import FastAPI, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text
from app.db import get_db
<<<<<<< HEAD
from app.routers import projects, memos
=======
from app.errors import ApiError, register_error_handlers
from app.routers import auth
>>>>>>> origin/main

app = FastAPI()
register_error_handlers(app)
app.include_router(auth.router)
app.include_router(auth.users_router)


@app.get("/health")
def health(db: Session = Depends(get_db)):
    try:
        db.execute(text("SELECT 1"))
    except Exception:
<<<<<<< HEAD
        raise HTTPException(status_code=503, detail={"status": "error", "db": "unreachable"})
    return {"status": "ok", "db": "ok"}
app.include_router(projects.router) 
app.include_router(memos.router)
=======
        raise ApiError(503, "DB_UNAVAILABLE", "잠시 후 다시 시도해 주세요")
    return {"status": "ok", "db": "ok"}
>>>>>>> origin/main
