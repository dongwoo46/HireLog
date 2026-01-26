import logging
from inputs.jd_preprocess_input import JdPreprocessInput
from url.fetcher import UrlFetcher
from url.playwright_fetcher import PlaywrightFetcher
from url.parser import UrlParser
from url.preprocessor import preprocess_url_text
from url.section_extractor import extract_url_sections
from preprocess.adapter.url_section_adapter import adapt_url_sections_to_sections
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor
from preprocess.worker.pipeline.canonical_section_pipeline import CanonicalSectionPipeline

logger = logging.getLogger(__name__)


class UrlPipeline:
    """
    URL 기반 JD 전처리 파이프라인

    핵심 전략 (OCR과 동일):
    - URL 전용 전처리로 노이즈 제거
    - Header 기반으로 섹션 분리 (구조 확정)
    - Core/Structural 단계 없이 바로 Canonical로 연결

    책임:
    - URL Fetching & Parsing
    - URL 전용 전처리 (UI 노이즈 제거)
    - Header 기반 섹션 분리
    - CanonicalSectionPipeline 연결
    """

    def __init__(self):
        self.fetcher = UrlFetcher()
        self.dynamic_fetcher = PlaywrightFetcher()
        self.parser = UrlParser()
        self.metadata = MetadataPreprocessor()
        self.canonical = CanonicalSectionPipeline()

    def process(self, input: JdPreprocessInput) -> dict:
        """
        URL → Fetch → Parse → 전처리 → Header 분리 → Canonical 흐름 실행
        """
        if not input.url:
            raise ValueError("url is required for UrlPipeline")

        # 1️⃣ Fetch (Hybrid Strategy)
        try:
            html_content = self.fetcher.fetch(input.url)

            # Check for JS Rendering Requirement
            if self.fetcher._needs_js_rendering(html_content):
                logger.info(f"JS Rendering required for {input.url}. Switching to Playwright.")
                html_content = self.dynamic_fetcher.fetch(input.url)

        except Exception as e:
            logger.warning(f"Static fetch failed for {input.url}, retrying with Playwright. Error: {e}")
            html_content = self.dynamic_fetcher.fetch(input.url)

        # 2️⃣ Parse (HTML → 텍스트)
        parsed_data = self.parser.parse(html_content)
        title = parsed_data.get("title", "")
        body_text = parsed_data.get("body", "")

        if not body_text:
            logger.warning(f"No body text extracted from URL: {input.url}")
            return {
                "url_meta": {
                    "url": input.url,
                    "title": title,
                    "fetched_length": len(html_content),
                    "parsed_length": 0
                },
                "canonical_map": {},
                "document_meta": None,
            }

        # 3️⃣ URL 전용 전처리 (노이즈 제거)
        cleaned_lines = preprocess_url_text(body_text)

        if not cleaned_lines:
            logger.warning(f"No lines after URL preprocessing: {input.url}")
            return {
                "url_meta": {
                    "url": input.url,
                    "title": title,
                    "fetched_length": len(html_content),
                    "parsed_length": len(body_text)
                },
                "canonical_map": {},
                "document_meta": None,
            }

        # 4️⃣ Header 기반 섹션 분리 (OCR 방식)
        raw_sections = extract_url_sections(cleaned_lines)

        if not raw_sections:
            logger.warning(f"No sections extracted from URL: {input.url}")
            return {
                "url_meta": {
                    "url": input.url,
                    "title": title,
                    "fetched_length": len(html_content),
                    "parsed_length": len(body_text)
                },
                "canonical_map": {},
                "document_meta": None,
            }

        # 5️⃣ Section 도메인 객체로 변환
        sections = adapt_url_sections_to_sections(raw_sections)

        # 6️⃣ Metadata 추출 (전처리된 라인 기준)
        document_meta = self.metadata.process(cleaned_lines)

        # 7️⃣ Canonical 후처리 (Semantic → Filter → Canonical)
        canonical_map = self.canonical.process(sections)

        # 8️⃣ 최종 결과
        return {
            "url_meta": {
                "url": input.url,
                "title": title,
                "fetched_length": len(html_content),
                "parsed_length": len(body_text),
                "cleaned_lines_count": len(cleaned_lines),
                "sections_count": len(raw_sections),
            },
            "canonical_map": canonical_map,
            "document_meta": document_meta,
        }
