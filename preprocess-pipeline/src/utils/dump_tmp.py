import json
from pathlib import Path
import numpy as np


# dump_tmp.py
# 위치: process-pipeline/src/utils/dump_tmp.py

# 프로젝트 루트: process-pipeline
PROJECT_ROOT = Path(__file__).resolve().parents[2]

# ✅ 우리가 원하는 정확한 경로
TMP_DIR = PROJECT_ROOT / "data" / "tmp"


def _json_safe(obj):
    if isinstance(obj, np.ndarray):
        return [_json_safe(v) for v in obj.tolist()]
    if isinstance(obj, np.generic):
        return obj.item()
    if isinstance(obj, dict):
        return {k: _json_safe(v) for k, v in obj.items()}
    if isinstance(obj, (list, tuple)):
        return [_json_safe(v) for v in obj]
    return obj


def _resolve_unique_path(base_dir: Path, name: str) -> Path:
    """
    name.json 이 없으면 그대로 사용
    있으면 name_001.json, name_002.json ...
    """
    base = base_dir / f"{name}.json"
    if not base.exists():
        return base

    idx = 1
    while True:
        candidate = base_dir / f"{name}_{idx:03d}.json"
        if not candidate.exists():
            return candidate
        idx += 1


def dump_tmp_data(name: str, data):
    """
    디버그용 임시 데이터 dump

    - 항상 data/tmp 에 저장
    - 같은 이름 존재 시 자동 증가
    """

    TMP_DIR.mkdir(parents=True, exist_ok=True)

    path = _resolve_unique_path(TMP_DIR, name)

    with open(path, "w", encoding="utf-8") as f:
        json.dump(
            _json_safe(data),
            f,
            ensure_ascii=False,
            indent=2
        )
