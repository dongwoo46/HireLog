import logging
import re
from typing import Callable, Optional
from urllib.parse import parse_qs, urlencode, urljoin, urlparse, urlunparse

from bs4 import BeautifulSoup

# OCR 관련 import는 함수 호출 시점에 lazy load한다.
# 모듈 레벨에서 import하면 text_url 프로세스가 PaddleOCR 전체를 불필요하게 로드한다.
from preprocess.adapter.ocr_section_adapter import adapt_ocr_sections_to_sections
from preprocess.post_validation.section_post_validator import validate_raw_sections

logger = logging.getLogger(__name__)


class JobKoreaUrlSupport:
    """JobKorea-specific URL helpers: URL normalize + OCR fallback orchestration."""

    def __init__(self, canonical_pipeline, normalize_required_fn: Callable[[dict], dict]):
        self.canonical = canonical_pipeline
        self.normalize_required = normalize_required_fn

    def normalize_doc_url(self, url: str) -> str:
        """Normalize JobKorea URL to GI_Read_Comt_Ifrm document URL when possible."""
        try:
            parsed = urlparse(url)
            host = (parsed.netloc or "").lower()
            if "jobkorea.co.kr" not in host:
                return url
            if "/Recruit/GI_Read_Comt_Ifrm" in (parsed.path or ""):
                return url
            if "/Recruit/GI_Read/" not in (parsed.path or ""):
                return url

            qs = parse_qs(parsed.query or "")
            gno = (qs.get("Gno") or [None])[0]
            if not gno:
                parts = [p for p in (parsed.path or "").split("/") if p]
                if parts:
                    gno = parts[-1]
            if not gno:
                return url

            new_qs = {k: v[0] for k, v in qs.items() if v}
            new_qs["Gno"] = gno
            rebuilt = parsed._replace(
                path="/Recruit/GI_Read_Comt_Ifrm",
                query=urlencode(new_qs),
                fragment="",
            )
            normalized = urlunparse(rebuilt)
            if normalized != url:
                logger.debug(
                    "JobKorea URL normalized to document URL",
                    extra={"input_url": url, "normalized_url": normalized},
                )
            return normalized
        except Exception:
            return url

    def collect_ocr_images(self, input_images: list | None, html: str, base_url: str) -> list[str]:
        images = list(input_images or [])
        images.extend(self._extract_image_urls_from_html(html, base_url))
        deduped = [u for i, u in enumerate(images) if u and u not in images[:i]]
        logger.debug(
            "JobKorea OCR image candidates prepared",
            extra={"url": base_url, "image_count": len(deduped)},
        )
        return deduped

    def ocr_only_result(self, images: list[str]) -> Optional[dict]:
        if not images:
            logger.debug("JobKorea OCR-only skipped: no images")
            return None

        from ocr.pipeline import process_ocr_input
        from ocr.structure.header_grouping import extract_sections_by_header
        # ppocr이 import/첫 추론 시 logging.disable()을 호출할 수 있으므로 OCR 후 즉시 복원
        import logging as _logging
        from utils.logger import setup_logging as _setup_logging
        try:
            ocr_result = process_ocr_input(images)
        except Exception as e:
            _setup_logging()
            _logging.disable(_logging.NOTSET)
            logger.warning("JobKorea OCR-only fallback failed", extra={"error": str(e)})
            return None
        _setup_logging()
        _logging.disable(_logging.NOTSET)

        if ocr_result.get("status") == "FAIL" or not ocr_result.get("lines"):
            errors = ocr_result.get("errors") or []
            logger.info(
                "JobKorea OCR-only produced no usable lines",
                extra={
                    "status": ocr_result.get("status"),
                    "confidence": round(ocr_result.get("confidence", 0), 2),
                    "line_count": len(ocr_result.get("lines", [])),
                    "error_count": len(errors),
                    "error_samples": errors[:2],
                },
            )
            return None

        raw_sections = extract_sections_by_header(ocr_result["lines"])
        raw_sections = validate_raw_sections(raw_sections)
        if not raw_sections:
            logger.info("JobKorea OCR-only raw sections empty after validation")
            return None

        ocr_sections = adapt_ocr_sections_to_sections(raw_sections)
        canonical_map = self.canonical.process(ocr_sections)
        return self.normalize_required(canonical_map)

    def ocr_fallback_merge(self, canonical_map: dict, images: list | None) -> dict:
        if not images:
            return canonical_map

        required_keys = ("responsibilities", "requirements", "preferred")
        if all(canonical_map.get(k) for k in required_keys):
            return canonical_map

        from ocr.pipeline import process_ocr_input
        from ocr.structure.header_grouping import extract_sections_by_header
        import logging as _logging
        from utils.logger import setup_logging as _setup_logging
        try:
            ocr_result = process_ocr_input(images)
        except Exception as e:
            _setup_logging()
            _logging.disable(_logging.NOTSET)
            logger.warning("JobKorea OCR fallback execution failed", extra={"error": str(e)})
            return canonical_map
        _setup_logging()
        _logging.disable(_logging.NOTSET)

        if ocr_result.get("status") == "FAIL" or not ocr_result.get("lines"):
            errors = ocr_result.get("errors") or []
            logger.info(
                "JobKorea OCR fallback skipped",
                extra={
                    "status": ocr_result.get("status"),
                    "confidence": round(ocr_result.get("confidence", 0), 2),
                    "line_count": len(ocr_result.get("lines", [])),
                    "error_count": len(errors),
                    "error_samples": errors[:2],
                },
            )
            return canonical_map

        raw_sections = extract_sections_by_header(ocr_result["lines"])
        raw_sections = validate_raw_sections(raw_sections)
        if not raw_sections:
            return canonical_map

        ocr_sections = adapt_ocr_sections_to_sections(raw_sections)
        ocr_canonical = self.canonical.process(ocr_sections)
        ocr_canonical = self.normalize_required(ocr_canonical)

        merged = {k: list(v) for k, v in canonical_map.items() if isinstance(v, list)}
        for key, ocr_values in ocr_canonical.items():
            if not isinstance(ocr_values, list):
                continue
            base_values = merged.get(key, [])
            if key in required_keys and not base_values:
                merged[key] = ocr_values
            elif key in required_keys and len(base_values) < len(ocr_values):
                merged[key] = ocr_values
            elif key not in merged:
                merged[key] = ocr_values

        logger.info(
            "JobKorea OCR fallback merged",
            extra={
                "image_count": len(images),
                "ocr_confidence": round(ocr_result.get("confidence", 0), 2),
                "required_after_merge": {k: len(merged.get(k, [])) for k in required_keys},
            },
        )
        return merged

    def _extract_image_urls_from_html(self, html: str, base_url: str) -> list[str]:
        if not html:
            return []
        try:
            soup = BeautifulSoup(html, "html.parser")
            urls: list[str] = []
            for img in soup.find_all("img"):
                src = (
                    img.get("src")
                    or img.get("data-src")
                    or img.get("data-original")
                    or img.get("data-lazy")
                    or img.get("data-lazy-src")
                    or ""
                ).strip()
                if not src:
                    continue
                full = urljoin(base_url, src)
                low = full.lower()
                if not any(ext in low for ext in (".jpg", ".jpeg", ".png", ".webp", ".bmp", "image", "upload")):
                    continue
                urls.append(full)

            for node in soup.find_all(style=True):
                style = (node.get("style") or "")
                m = re.search(r"background-image\s*:\s*url\((['\"]?)(.+?)\1\)", style, re.IGNORECASE)
                if not m:
                    continue
                full = urljoin(base_url, m.group(2).strip())
                low = full.lower()
                if any(ext in low for ext in (".jpg", ".jpeg", ".png", ".webp", ".bmp", "image", "upload")):
                    urls.append(full)

            return urls
        except Exception:
            return []
