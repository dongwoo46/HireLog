import logging
import os
import re

from domain.job_platform import JobPlatform
from inputs.jd_preprocess_input import JdPreprocessInput
from ocr.grpc.ocr_client import OcrGrpcClient
from preprocess.adapter.url_section_adapter import adapt_url_sections_to_sections
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor
from preprocess.post_validation.section_post_validator import validate_raw_sections
from preprocess.worker.pipeline.canonical_section_pipeline import CanonicalSectionPipeline
from preprocess.worker.pipeline.jobkorea_url_support import JobKoreaUrlSupport
from url.fetcher import UrlFetcher
from url.fetchers import PlaywrightFetcher, SaraminPlaywrightFetcher
from url.parsers import JobKoreaUrlParser, SaraminUrlParser, UrlParser
from url.preprocessor import get_platform_module, preprocess_url_text

logger = logging.getLogger(__name__)

_ZERO_WIDTH_CHARS = "".join(chr(cp) for cp in (0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF))
_ZERO_WIDTH_RE = re.compile(f"[{re.escape(_ZERO_WIDTH_CHARS)}]")
_BULLET_PREFIX_RE = re.compile(r"^[\s\-*•]+")


class UrlPipeline:
    def __init__(self):
        self.fetcher = UrlFetcher()
        self.dynamic_fetcher = PlaywrightFetcher()
        self.saramin_dynamic_fetcher = SaraminPlaywrightFetcher()

        self.default_parser = UrlParser()
        self.saramin_parser = SaraminUrlParser()
        self.jobkorea_parser = JobKoreaUrlParser()

        self.metadata = MetadataPreprocessor()
        self.canonical = CanonicalSectionPipeline()

        ocr_grpc_host = os.environ.get("OCR_GRPC_HOST", "localhost")
        ocr_grpc_port = int(os.environ.get("OCR_GRPC_PORT", "50051"))
        ocr_client = OcrGrpcClient(host=ocr_grpc_host, port=ocr_grpc_port)

        self.jobkorea_support = JobKoreaUrlSupport(
            self.canonical,
            self._normalize_intake_required_canonical,
            ocr_client=ocr_client,
        )

    def process(self, input: JdPreprocessInput) -> dict:
        if not input.url:
            raise ValueError("url is required for UrlPipeline")

        effective_platform = JobPlatform.from_url(input.url)
        fetch_url = self.jobkorea_support.normalize_doc_url(input.url) if effective_platform == JobPlatform.JOBKOREA else input.url

        # 1) Fetch
        try:
            html_content = self.fetcher.fetch(fetch_url)

            if effective_platform == JobPlatform.SARAMIN:
                logger.debug(
                    "Saramin request uses dedicated Playwright fetcher",
                    extra={"url": input.url, "static_html_length": len(html_content or "")},
                )
                html_content = self.saramin_dynamic_fetcher.fetch(input.url)
            elif self.fetcher._needs_js_rendering(html_content):
                logger.debug("JS rendering required, switching to Playwright", extra={"url": fetch_url})
                html_content = self.dynamic_fetcher.fetch(fetch_url)

        except Exception as e:
            logger.warning(
                "Static fetch failed, retrying with Playwright",
                extra={"url": fetch_url, "error": str(e)},
            )
            if effective_platform == JobPlatform.SARAMIN:
                html_content = self.saramin_dynamic_fetcher.fetch(input.url)
            else:
                html_content = self.dynamic_fetcher.fetch(fetch_url)

        # 2) Parse
        if effective_platform == JobPlatform.SARAMIN:
            parser = self.saramin_parser
        elif effective_platform == JobPlatform.JOBKOREA:
            parser = self.jobkorea_parser
        else:
            parser = self.default_parser

        parsed_data = parser.parse(html_content, url=fetch_url)
        title = parsed_data.get("title", "")
        body_text = parsed_data.get("body", "")

        ocr_images = []
        if effective_platform == JobPlatform.JOBKOREA:
            ocr_images = self.jobkorea_support.collect_ocr_images(input.images, html_content, fetch_url)

        logger.debug(
            "URL fetched and parsed",
            extra={
                "url": fetch_url,
                "title": title,
                "fetched_length": len(html_content),
                "parsed_length": len(body_text),
            },
        )

        # JobKorea image-only docs: run OCR immediately when body text is empty.
        if not body_text:
            if effective_platform == JobPlatform.JOBKOREA:
                ocr_only = self.jobkorea_support.ocr_only_result(ocr_images)
                if ocr_only is not None:
                    logger.info(
                        "JobKorea OCR-only fallback used due to empty body",
                        extra={"url": fetch_url, "image_count": len(ocr_images)},
                    )
                    return {
                        "url_meta": {
                            "url": input.url,
                            "fetched_url": fetch_url,
                            "title": title,
                            "fetched_length": len(html_content),
                            "parsed_length": 0,
                        },
                        "canonical_map": self.canonical.to_jobsummary_canonical_map(ocr_only),
                        "document_meta": None,
                    }

                logger.warning(
                    "JobKorea OCR-only fallback failed on empty body",
                    extra={"url": fetch_url, "image_count": len(ocr_images)},
                )

            logger.warning("No body text extracted from URL", extra={"url": fetch_url})
            return {
                "url_meta": {
                    "url": input.url,
                    "fetched_url": fetch_url,
                    "title": title,
                    "fetched_length": len(html_content),
                    "parsed_length": 0,
                },
                "canonical_map": {},
                "document_meta": None,
            }

        # 3) Preprocess lines
        cleaned_lines = preprocess_url_text(body_text, platform=effective_platform)
        if not cleaned_lines:
            logger.warning(
                "No lines after URL preprocessing",
                extra={"url": fetch_url, "parsed_length": len(body_text)},
            )
            return {
                "url_meta": {
                    "url": input.url,
                    "fetched_url": fetch_url,
                    "title": title,
                    "fetched_length": len(html_content),
                    "parsed_length": len(body_text),
                },
                "canonical_map": {},
                "document_meta": None,
            }

        # 4) Extract sections
        platform_mod = get_platform_module(effective_platform)
        raw_sections = platform_mod.extract_sections(cleaned_lines)
        raw_sections = validate_raw_sections(raw_sections)

        if not raw_sections:
            logger.warning(
                "No sections extracted from URL",
                extra={"url": fetch_url, "cleaned_line_count": len(cleaned_lines)},
            )
            return {
                "url_meta": {
                    "url": input.url,
                    "fetched_url": fetch_url,
                    "title": title,
                    "fetched_length": len(html_content),
                    "parsed_length": len(body_text),
                },
                "canonical_map": {},
                "document_meta": None,
            }

        # 5) To domain sections
        sections = adapt_url_sections_to_sections(raw_sections)

        # 6) Metadata
        document_meta = self.metadata.process(cleaned_lines)

        # 7) Canonical
        canonical_map = self.canonical.process(sections)
        canonical_map = self._normalize_intake_required_canonical(canonical_map)
        if effective_platform == JobPlatform.JOBKOREA:
            canonical_map = self.jobkorea_support.ocr_fallback_merge(canonical_map, ocr_images)
        canonical_map = self.canonical.to_jobsummary_canonical_map(canonical_map)

        # 8) Done
        logger.info(
            "URL pipeline completed",
            extra={
                "url": input.url,
                "fetched_url": fetch_url,
                "platform": effective_platform.value,
                "fetched_length": len(html_content),
                "parsed_length": len(body_text),
                "cleaned_lines_count": len(cleaned_lines),
                "sections_count": len(raw_sections),
                "canonical_keys": list(canonical_map.keys()),
            },
        )

        return {
            "url_meta": {
                "url": input.url,
                "fetched_url": fetch_url,
                "title": title,
                "fetched_length": len(html_content),
                "parsed_length": len(body_text),
                "cleaned_lines_count": len(cleaned_lines),
                "sections_count": len(raw_sections),
            },
            "canonical_map": canonical_map,
            "document_meta": document_meta,
        }

    def _normalize_intake_required_canonical(self, canonical_map: dict) -> dict:
        if not canonical_map:
            return canonical_map

        merged = {k: list(v) for k, v in canonical_map.items() if isinstance(v, list)}
        all_lines: list[str] = []
        for lines in merged.values():
            all_lines.extend([line for line in lines if isinstance(line, str)])

        resp_kw = ("주요업무", "담당업무", "업무", "직무", "responsibilities", "role", "what you will do")
        req_kw = ("자격요건", "지원자격", "필수", "requirements", "qualifications", "must", "required")
        pref_kw = ("우대", "우대사항", "preferred", "nice to have", "plus")
        stop_header_kw = (
            "혜택", "복지", "근무조건", "채용전형", "채용절차", "전형절차", "유의사항",
            "접수기간", "제출서류", "접수방법", "근무지", "고용형태", "location",
            "benefits", "process", "how to apply", "application guide",
        )

        weak_noise_kw = (
            "근무조건", "복지", "혜택", "채용절차", "전형절차", "유의사항",
            "접수기간", "제출서류", "접수방법",
        )

        def normalize_text(text: str) -> str:
            s = _ZERO_WIDTH_RE.sub("", text or "")
            s = re.sub(r"\s+", " ", s).strip().lower()
            return s

        def normalize_compact(text: str) -> str:
            return normalize_text(text).replace(" ", "")

        def strip_bullet(text: str) -> str:
            return _BULLET_PREFIX_RE.sub("", text or "").strip()

        resp_markers = ("주요업무", "담당업무", "responsibilities", "whatyouwilldo", "role")
        req_markers = ("자격요건", "지원자격", "requirements", "qualifications", "musthave", "required")
        pref_markers = ("우대사항", "우대", "preferred", "nicetohave", "plus")
        stop_markers = tuple(normalize_compact(k) for k in stop_header_kw)

        def detect_marker(line: str) -> str | None:
            compact = normalize_compact(strip_bullet(line)).strip("[]()<>")
            if not compact:
                return None
            if len(compact) > 40:
                return None

            def _match(markers: tuple[str, ...]) -> bool:
                return any(compact == m for m in markers)

            if _match(resp_markers):
                return "responsibilities"
            if _match(req_markers):
                return "requirements"
            if _match(pref_markers):
                return "preferred"
            return None

        def is_stop_header(line: str) -> bool:
            compact = normalize_compact(strip_bullet(line)).strip("[]()<>")
            if not compact or len(compact) > 40:
                return False
            return any(compact == m for m in stop_markers)

        def trim_at_stop(lines: list[str]) -> list[str]:
            out: list[str] = []
            for line in lines:
                if is_stop_header(line):
                    break
                out.append(line)
            return out

        resp_lines = trim_at_stop(list(merged.get("responsibilities", [])))
        req_lines = trim_at_stop(list(merged.get("requirements", [])))
        pref_lines = trim_at_stop(list(merged.get("preferred", [])))

        # Marker-based reparsing for broken boundaries:
        ordered_lines: list[str] = []
        for key in (
            "responsibilities", "requirements", "preferred",
            "intro", "others", "experience", "skills", "process",
            "benefits", "application_guide", "employment_type", "location",
        ):
            ordered_lines.extend([line for line in merged.get(key, []) if isinstance(line, str)])

        reparsed = {"responsibilities": [], "requirements": [], "preferred": []}
        current_zone: str | None = None
        for line in ordered_lines:
            marker = detect_marker(line)
            if marker:
                current_zone = marker
                continue
            if is_stop_header(line):
                current_zone = None
                continue
            if current_zone in reparsed:
                reparsed[current_zone].append(line)

        if reparsed["responsibilities"]:
            resp_lines = reparsed["responsibilities"]
        if reparsed["requirements"]:
            req_lines = reparsed["requirements"]
        if reparsed["preferred"]:
            pref_lines = reparsed["preferred"]

        moved_from_req = []
        for line in req_lines:
            low = normalize_text(line)
            if any(k in low for k in pref_kw):
                pref_lines.append(line)
                moved_from_req.append(line)
            elif any(k in low for k in resp_kw):
                resp_lines.append(line)
                moved_from_req.append(line)
        if moved_from_req:
            moved_set = set(moved_from_req)
            req_lines = [line for line in req_lines if line not in moved_set]

        if not resp_lines:
            resp_lines.extend([line for line in all_lines if any(k in normalize_text(line) for k in resp_kw)])
        if not req_lines:
            req_lines.extend([line for line in all_lines if any(k in normalize_text(line) for k in req_kw)])
        if not pref_lines:
            pref_lines.extend([line for line in all_lines if any(k in normalize_text(line) for k in pref_kw)])

        fallback_pool = []
        for key in ("others", "experience", "intro", "skills"):
            fallback_pool.extend([line for line in merged.get(key, []) if isinstance(line, str)])
        fallback_pool = [line for line in fallback_pool if not any(k in line for k in weak_noise_kw)]

        if not resp_lines and fallback_pool:
            resp_lines.extend(fallback_pool[:3])
        if not req_lines and fallback_pool:
            req_lines.extend(fallback_pool[3:6] or fallback_pool[:2])
        if not pref_lines and fallback_pool:
            pref_lines.extend(fallback_pool[6:8] or fallback_pool[:1])

        if not resp_lines:
            resp_lines = [line for line in all_lines if len((line or "").strip()) >= 8][:2]
        if not req_lines:
            req_lines = [line for line in all_lines if len((line or "").strip()) >= 8][2:4] or resp_lines[:1]
        if not pref_lines:
            pref_lines = [line for line in all_lines if len((line or "").strip()) >= 8][4:5] or req_lines[:1]

        def clean(lines: list[str]) -> list[str]:
            out = []
            seen = set()
            for line in lines:
                if detect_marker(line) is not None:
                    continue
                if is_stop_header(line):
                    continue
                s = _ZERO_WIDTH_RE.sub("", (line or ""))
                s = re.sub(r"\s+", " ", s).strip()
                if len(s) < 2:
                    continue
                key = s.lower()
                if key in seen:
                    continue
                seen.add(key)
                out.append(s)
            return out

        merged["responsibilities"] = clean(resp_lines)
        merged["requirements"] = clean(req_lines)
        merged["preferred"] = clean(pref_lines)
        return merged
