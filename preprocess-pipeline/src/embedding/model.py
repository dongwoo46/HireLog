# src/embedding/model.py

import logging
import torch
from sentence_transformers import SentenceTransformer
from embedding.config import config

logger = logging.getLogger(__name__)

_model: SentenceTransformer | None = None


def _resolve_device() -> str:
    if torch.cuda.is_available():
        return "cuda"
    if torch.backends.mps.is_available():
        return "mps"
    return "cpu"


def load_model() -> SentenceTransformer:
    global _model
    if _model is not None:
        return _model

    device = _resolve_device()
    logger.info("[EMBEDDING_MODEL_LOAD] model=%s, device=%s", config.model_name, device)

    _model = SentenceTransformer(config.model_name, device=device)

    # 첫 요청 레이턴시 제거용 워밍업
    _model.encode("warmup", normalize_embeddings=True)
    logger.info("[EMBEDDING_MODEL_READY] model=%s, device=%s", config.model_name, device)

    return _model


def get_model() -> SentenceTransformer:
    if _model is None:
        raise RuntimeError("Model not loaded. Call load_model() first.")
    return _model
