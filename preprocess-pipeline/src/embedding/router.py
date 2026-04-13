# src/embedding/router.py

import logging
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from embedding.model import get_model
from embedding.config import config

logger = logging.getLogger(__name__)

router = APIRouter()


class EmbedRequest(BaseModel):
    responsibilities: str
    requiredQualifications: str
    preferredQualifications: str | None = None
    idealCandidate: str | None = None
    mustHaveSignals: str | None = None
    technicalContext: str | None = None


class EmbedResponse(BaseModel):
    vector: list[float]
    dim: int
    model: str


def _build_embed_text(req: EmbedRequest) -> str:
    parts = [
        req.responsibilities,
        req.requiredQualifications,
    ]
    if req.preferredQualifications:
        parts.append(req.preferredQualifications)
    if req.idealCandidate:
        parts.append(req.idealCandidate)
    if req.mustHaveSignals:
        parts.append(req.mustHaveSignals)
    if req.technicalContext:
        parts.append(req.technicalContext)
    return "\n".join(parts)


@router.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest) -> EmbedResponse:
    text = _build_embed_text(req)

    if not text.strip():
        raise HTTPException(status_code=400, detail="all fields are empty")

    model = get_model()
    vector = model.encode(text, normalize_embeddings=True).tolist()

    logger.info("[EMBEDDING_SUCCESS] text_length=%d, vector_dim=%d", len(text), len(vector))

    return EmbedResponse(vector=vector, dim=len(vector), model=config.model_name)


class QueryEmbedRequest(BaseModel):
    text: str


@router.post("/embed/query", response_model=EmbedResponse)
def embed_query(req: QueryEmbedRequest) -> EmbedResponse:
    if not req.text or not req.text.strip():
        raise HTTPException(status_code=400, detail="text must not be empty")

    model = get_model()
    vector = model.encode(req.text, normalize_embeddings=True).tolist()

    logger.info("[EMBEDDING_QUERY_SUCCESS] text_length=%d, vector_dim=%d", len(req.text), len(vector))

    return EmbedResponse(vector=vector, dim=len(vector), model=config.model_name)
