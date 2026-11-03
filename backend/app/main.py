from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from sqlalchemy import text
from app.db import get_db
from app.routers import memos, projects 

app = FastAPI(
    title="HankoVillage API Server",
    description="한코마을 앱 백엔드 API 서비스",
    version="1.0.0"
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
def health(db: Session = Depends(get_db)):
    try:
        db.execute(text("SELECT 1"))
    except Exception:
        raise HTTPException(status_code=503, detail={"status": "error", "db": "unreachable"})
    return {"status": "ok", "db": "ok"}

app.include_router(projects.router) 
app.include_router(memos.router)