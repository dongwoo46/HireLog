import logging
import os
import tempfile
from urllib.parse import urlparse

import requests

from normalize.pipeline import normalize_lines
from ocr.confidence import classify_confidence
from ocr.engine import run_ocr
from ocr.header_detector import detect_visual_headers
from ocr.lines import build_lines
from ocr.postprocess import postprocess_ocr_lines
from ocr.preprocess import preprocess_image
from ocr.quality import filter_low_quality_lines
from utils.rawtext import build_raw_text

logger = logging.getLogger(__name__)


def process_ocr_input(image_input: str | list[str]) -> dict:
    image_paths = normalize_image_paths(image_input)
    if not image_paths:
        return _fail("no image paths provided")

    aggregated_raw_texts = []
    aggregated_lines = []
    confidences = []
    errors = []

    for path in image_paths:
        result = _process_single_image(path)
        if result.get("error"):
            errors.append({"path": path, "error": result["error"]})
            continue

        if result["rawText"]:
            aggregated_raw_texts.append(result["rawText"])
        if result["lines"]:
            aggregated_lines.extend(result["lines"])
        if result["confidence"] > 0:
            confidences.append(result["confidence"])

    final_raw_text = "\n\n".join(aggregated_raw_texts)
    final_confidence = sum(confidences) / len(confidences) if confidences else 0.0
    final_status = classify_confidence(final_confidence).value

    return {
        "rawText": final_raw_text,
        "lines": aggregated_lines,
        "confidence": final_confidence,
        "status": final_status,
        "errors": errors,
    }


def _fail(reason: str) -> dict:
    return {
        "rawText": "",
        "lines": [],
        "confidence": 0.0,
        "status": "FAIL",
        "error": reason,
    }


def _process_single_image(image_path: str) -> dict:
    temp_local_path = None
    local_image_path = image_path

    if _is_remote_image_path(image_path):
        downloaded = _download_remote_image_to_temp(image_path)
        if downloaded.get("error"):
            return _fail(downloaded["error"])
        local_image_path = downloaded["path"]
        temp_local_path = local_image_path

    try:
        if not isinstance(local_image_path, str):
            return _fail("image_path is not a string")
        if not os.path.exists(local_image_path):
            return _fail(f"image not found: {local_image_path}")
        if not os.path.isfile(local_image_path):
            return _fail(f"image_path is not a file: {local_image_path}")
        if not os.access(local_image_path, os.R_OK):
            return _fail(f"image not readable: {local_image_path}")

        try:
            preprocessed_image = preprocess_image(local_image_path)
        except Exception as e:
            return _fail(f"preprocess_image failed: {e}")

        ocr_result = run_ocr(preprocessed_image)
        if not ocr_result.get("raw"):
            return _fail("ocr returned empty raw result")

        logger.debug(
            "OCR raw result",
            extra={
                "raw_line_count": len(ocr_result["raw"]),
                "confidence": round(ocr_result.get("confidence", 0), 2),
            },
        )

        lines = build_lines(ocr_result["raw"])
        logger.debug("OCR lines built", extra={"line_count": len(lines)})

        lines = detect_visual_headers(lines)
        normalized = normalize_lines(lines)
        passed_lines, dropped_lines = filter_low_quality_lines(
            normalized,
            min_confidence=45,
            max_garbage_ratio=0.6,
        )

        ocr_lines = postprocess_ocr_lines(passed_lines)
        logger.debug(
            "OCR postprocess completed",
            extra={
                "passed_line_count": len(ocr_lines),
                "dropped_line_count": len(dropped_lines),
            },
        )

        raw_text = build_raw_text(ocr_lines)
        status = classify_confidence(ocr_result["confidence"]).value

        return {
            "rawText": raw_text,
            "lines": ocr_lines,
            "confidence": ocr_result["confidence"],
            "status": status,
        }
    finally:
        if temp_local_path and os.path.exists(temp_local_path):
            try:
                os.remove(temp_local_path)
            except Exception:
                pass


def normalize_image_paths(image_input) -> list[str]:
    paths: list[str] = []
    if isinstance(image_input, str):
        paths = image_input.split(",")
    elif isinstance(image_input, list):
        for item in image_input:
            if not item:
                continue
            if isinstance(item, str) and "," in item:
                paths.extend(item.split(","))
            else:
                paths.append(str(item))
    return [p.strip() for p in paths if p and p.strip()]


def _is_remote_image_path(path: str) -> bool:
    if not isinstance(path, str):
        return False
    scheme = (urlparse(path).scheme or "").lower()
    return scheme in ("http", "https")


def _download_remote_image_to_temp(url: str) -> dict:
    try:
        parsed = urlparse(url)
        referer = f"{parsed.scheme}://{parsed.netloc}/" if parsed.scheme and parsed.netloc else ""
        host = (parsed.netloc or "").lower()
        if "jobkorea.co.kr" in host:
            # JobKorea image endpoints often require a main-site referer.
            referer = "https://www.jobkorea.co.kr/"
        headers = {
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            )
        }
        if referer:
            headers["Referer"] = referer

        resp = requests.get(url, headers=headers, timeout=15)
        resp.raise_for_status()

        suffix = ".jpg"
        content_type = (resp.headers.get("Content-Type") or "").lower()
        # Some job-site image endpoints return octet-stream for image bytes.
        # Accept octet-stream and infer by magic bytes when needed.
        raw = resp.content or b""
        is_octet_stream = "application/octet-stream" in content_type
        if content_type and ("image" not in content_type) and not is_octet_stream:
            return {"error": f"remote image download failed: non-image content-type ({content_type})"}

        if "png" in content_type or raw.startswith(b"\x89PNG"):
            suffix = ".png"
        elif "webp" in content_type or raw.startswith(b"RIFF") and raw[8:12] == b"WEBP":
            suffix = ".webp"
        elif "bmp" in content_type or raw.startswith(b"BM"):
            suffix = ".bmp"
        elif raw.startswith(b"\xff\xd8\xff"):
            suffix = ".jpg"
        elif "jpeg" in content_type or "jpg" in content_type:
            suffix = ".jpg"

        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            tmp.write(raw)
            tmp_path = tmp.name

        return {"path": tmp_path}
    except Exception as e:
        return {"error": f"remote image download failed: {e}"}
