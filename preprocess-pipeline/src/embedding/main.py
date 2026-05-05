# src/embedding/main.py

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.logger import setup_logging
setup_logging()

import logging
import uvicorn
from contextlib import asynccontextmanager
from fastapi import FastAPI
from embedding.config import config
from embedding.model import load_model
from embedding.router import router
from embedding.admin_router import router as admin_router

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    load_model()
    yield


app = FastAPI(lifespan=lifespan)
app.include_router(router)
app.include_router(admin_router)


if __name__ == "__main__":
    uvicorn.run(
        "embedding.main:app",
        host=config.host,
        port=config.port,
        log_config=None,  # 기존 JSON 로거 유지
    )
