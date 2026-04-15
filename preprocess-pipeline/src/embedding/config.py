# src/embedding/config.py

import os


class EmbeddingConfig:
    model_name: str = os.environ.get("EMBEDDING_MODEL", "jhgan/ko-sroberta-multitask")
    host: str = os.environ.get("EMBEDDING_SERVER_HOST", "0.0.0.0")
    port: int = int(os.environ.get("EMBEDDING_SERVER_PORT", "8000"))


config = EmbeddingConfig()
