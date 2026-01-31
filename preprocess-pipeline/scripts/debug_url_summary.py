# scripts/debug_url_summary.py

"""
URL Pipeline 디버그 스크립트

새로운 URL 전용 파이프라인 (OCR 방식):
1. URL Fetch (정적/동적)
2. HTML Parse
3. URL 전용 전처리 (노이즈 제거)
4. Header 기반 섹션 분리
5. Section 객체 변환
6. Metadata 추출
7. Semantic-lite (의미 구역 보정)
8. Filter (불필요 섹션 제거)
9. Canonical Map 생성

사용법:
  # URL 직접 테스트 (전체 과정 출력)
  python scripts/debug_url_summary.py --url "https://example.com/job"

  # 테스트 URL 2개 자동 실행
  python scripts/debug_url_summary.py --test

  # Redis Consumer 모드 (Redis 필요)
  python scripts/debug_url_summary.py --consumer
"""

import sys
import os
import logging
import time
import argparse

# ==================================================
# src 경로를 PYTHONPATH에 추가
# ==================================================
sys.path.append(
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src"))
)

# 캐시 클리어 (yml 파일 수정 반영 위해)
from common.section.loader import clear_keyword_cache, load_header_keywords
clear_keyword_cache()

# 개별 컴포넌트 import (상세 디버깅용)
from url.fetcher import UrlFetcher
from url.playwright_fetcher import PlaywrightFetcher
from url.parser import UrlParser
from url.preprocessor import preprocess_url_text
from url.section_extractor import extract_url_sections
from preprocess.adapter.url_section_adapter import adapt_url_sections_to_sections
from preprocess.semantic.semantic_preprocessor import apply_semantic_lite
from preprocess.semantic.section_filter import filter_irrelevant_sections
from preprocess.worker.pipeline.canonical_section_pipeline import CanonicalSectionPipeline
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)


# ==================================================
# 출력 헬퍼 함수
# ==================================================

def print_header(title: str, char: str = "=", width: int = 80):
    """섹션 헤더 출력"""
    print(f"\n{char * width}")
    print(f" {title}")
    print(f"{char * width}")


def print_subheader(title: str, char: str = "-", width: int = 60):
    """서브 헤더 출력"""
    print(f"\n{char * width}")
    print(f" {title}")
    print(f"{char * width}")


def print_lines(lines: list, max_items: int = 20, prefix: str = "  "):
    """라인 목록 출력"""
    for i, line in enumerate(lines[:max_items]):
        truncated = line[:100] + "..." if len(line) > 100 else line
        print(f"{prefix}[{i+1:3d}] {truncated}")
    if len(lines) > max_items:
        print(f"{prefix}... and {len(lines) - max_items} more lines")


def print_section(section, index: int):
    """Section 객체 상세 출력"""
    print(f"\n  [{index}] Header: {section.header or '(none)'}")
    print(f"      Semantic Zone: {section.semantic_zone}")
    print(f"      Lines ({len(section.lines)}):")
    for line in section.lines[:5]:
        truncated = line[:80] + "..." if len(line) > 80 else line
        print(f"        - {truncated}")
    if len(section.lines) > 5:
        print(f"        ... and {len(section.lines) - 5} more lines")

    if section.lists:
        print(f"      Lists ({len(section.lists)} groups):")
        for lst_idx, lst in enumerate(section.lists[:3]):
            print(f"        List[{lst_idx}]: {len(lst)} items")
            for item in lst[:3]:
                truncated = item[:60] + "..." if len(item) > 60 else item
                print(f"          - {truncated}")
            if len(lst) > 3:
                print(f"          ... and {len(lst) - 3} more items")
        if len(section.lists) > 3:
            print(f"        ... and {len(section.lists) - 3} more list groups")


def print_raw_sections(raw_sections: dict):
    """Header 기반 섹션 분리 결과 출력"""
    for header, lines in raw_sections.items():
        print(f"\n  [{header}] ({len(lines)} lines)")
        for line in lines[:5]:
            truncated = line[:80] + "..." if len(line) > 80 else line
            print(f"    - {truncated}")
        if len(lines) > 5:
            print(f"    ... and {len(lines) - 5} more lines")


# ==================================================
# URL 상세 디버그 테스트 (새 파이프라인)
# ==================================================

def test_url_pipeline_verbose(url: str, brand_name: str = "TestBrand", position_name: str = "TestPosition"):
    """URL 파이프라인 상세 디버그 (OCR 방식 새 파이프라인)"""

    print_header(f"URL Pipeline Debug (NEW): {url[:60]}...")

    # ==================================================
    # STEP 1: URL Fetch
    # ==================================================
    print_header("STEP 1: URL FETCH", "=")

    fetcher = UrlFetcher()
    dynamic_fetcher = PlaywrightFetcher()

    fetch_method = "static"
    try:
        html_content = fetcher.fetch(url)

        # JS 렌더링 필요 여부 체크
        if fetcher._needs_js_rendering(html_content):
            print("  [INFO] JS Rendering required. Switching to Playwright...")
            html_content = dynamic_fetcher.fetch(url)
            fetch_method = "playwright"
        else:
            print("  [INFO] Static fetch successful")

    except Exception as e:
        print(f"  [WARN] Static fetch failed: {e}")
        print("  [INFO] Retrying with Playwright...")
        html_content = dynamic_fetcher.fetch(url)
        fetch_method = "playwright"

    print(f"\n  Fetch Method: {fetch_method}")
    print(f"  HTML Length: {len(html_content):,} chars")
    print(f"\n  [Preview - First 500 chars]")
    print("-" * 60)
    print(html_content[:500])
    print("-" * 60)

    # ==================================================
    # STEP 2: HTML Parse
    # ==================================================
    print_header("STEP 2: HTML PARSE", "=")

    parser = UrlParser()
    parsed_data = parser.parse(html_content)
    title = parsed_data.get("title", "")
    body_text = parsed_data.get("body", "")

    print(f"  Title: {title}")
    print(f"  Body Length: {len(body_text):,} chars")

    # Body가 너무 짧거나 핵심 키워드가 없으면 Playwright로 재시도
    MIN_BODY_LENGTH = 500
    JD_KEYWORDS = ["우대사항", "우대 사항", "preferred", "nice to have"]
    has_jd_keywords = any(kw in body_text for kw in JD_KEYWORDS)

    needs_playwright = (
        len(body_text) < MIN_BODY_LENGTH or
        (not has_jd_keywords and "wanted.co.kr" in url)  # 원티드는 버튼 클릭 필요
    )

    if needs_playwright and fetch_method == "static":
        reason = "Body too short" if len(body_text) < MIN_BODY_LENGTH else "Missing JD keywords (우대사항 등)"
        print(f"  [WARN] {reason}. Retrying with Playwright...")
        html_content = dynamic_fetcher.fetch(url)
        fetch_method = "playwright"
        parsed_data = parser.parse(html_content)
        title = parsed_data.get("title", "")
        body_text = parsed_data.get("body", "")
        print(f"  [INFO] After Playwright: Body Length: {len(body_text):,} chars")

    print(f"\n  [Body Preview - First 2000 chars]")
    print("-" * 60)
    try:
        print(body_text[:2000])
    except UnicodeEncodeError:
        print(body_text[:2000].encode('utf-8', errors='replace').decode('utf-8'))
    print("-" * 60)

    # ==================================================
    # STEP 3: URL 전용 전처리 (노이즈 제거)
    # ==================================================
    print_header("STEP 3: URL PREPROCESS (Noise Removal)", "=")

    cleaned_lines = preprocess_url_text(body_text)
    metadata_preprocessor = MetadataPreprocessor()
    document_meta = metadata_preprocessor.process(cleaned_lines)

    print(f"  Before: {len(body_text.split(chr(10)))} lines")
    print(f"  After: {len(cleaned_lines)} lines")
    print(f"\n  [Cleaned Lines Preview]")
    print_lines(cleaned_lines, max_items=30)

    # ==================================================
    # STEP 4: Header 기반 섹션 분리 (OCR 방식)
    # ==================================================
    print_header("STEP 4: HEADER-BASED SECTION EXTRACTION", "=")

    # 로드된 헤더 키워드 확인
    header_keywords = load_header_keywords()
    print(f"  [DEBUG] Loaded header keywords ({len(header_keywords)}):")
    # 주요 키워드만 출력
    important_keywords = [kw for kw in header_keywords if any(
        k in kw for k in ["자격", "우대", "주요", "업무", "채용", "프로세스", "요건"]
    )]
    for kw in sorted(important_keywords)[:20]:
        print(f"    - '{kw}'")

    raw_sections = extract_url_sections(cleaned_lines)

    print(f"\n  Total Sections: {len(raw_sections)}")
    print_raw_sections(raw_sections)

    # ==================================================
    # STEP 5: Section 객체 변환
    # ==================================================
    print_header("STEP 5: SECTION OBJECT CONVERSION", "=")

    sections = adapt_url_sections_to_sections(raw_sections)

    print(f"  Total Section Objects: {len(sections)}")
    for i, sec in enumerate(sections):
        print_section(sec, i + 1)

    # ==================================================
    # STEP 6: Metadata 추출
    # ==================================================
    print_header("STEP 6: METADATA EXTRACT", "=")

    metadata_processor = MetadataPreprocessor()
    document_meta = metadata_processor.process(cleaned_lines)

    if document_meta:
        if hasattr(document_meta, 'recruitment_period') and document_meta.recruitment_period:
            period = document_meta.recruitment_period
            print(f"  Recruitment Period Type: {period.period_type}")
            print(f"  Open Date: {period.open_date}")
            print(f"  Close Date: {period.close_date}")
        if hasattr(document_meta, 'skill_set') and document_meta.skill_set:
            print(f"  Skills: {document_meta.skill_set.skills}")
    else:
        print("  (No metadata extracted)")

    # ==================================================
    # STEP 7: Semantic-lite (의미 구역 보정)
    # ==================================================
    print_header("STEP 7: SEMANTIC-LITE (Zone Assignment)", "=")

    semantic_sections = apply_semantic_lite(sections)

    print(f"  Sections after semantic-lite: {len(semantic_sections)}")
    print("\n  [Semantic Zone Summary]")

    zone_summary = {}
    for sec in semantic_sections:
        zone = sec.semantic_zone
        if zone not in zone_summary:
            zone_summary[zone] = []
        zone_summary[zone].append(sec.header or "(no header)")

    for zone, headers in zone_summary.items():
        print(f"\n  Zone: {zone}")
        for h in headers[:5]:
            print(f"    - {h}")
        if len(headers) > 5:
            print(f"    ... and {len(headers) - 5} more")

    # ==================================================
    # STEP 8: Filter Irrelevant Sections
    # ==================================================
    print_header("STEP 8: FILTER IRRELEVANT SECTIONS", "=")

    filtered_sections = filter_irrelevant_sections(semantic_sections)

    print(f"  Before filter: {len(semantic_sections)} sections")
    print(f"  After filter: {len(filtered_sections)} sections")
    print(f"  Removed: {len(semantic_sections) - len(filtered_sections)} sections")

    # 제거된 섹션 표시
    filtered_headers = {sec.header for sec in filtered_sections}
    removed = [sec for sec in semantic_sections if sec.header not in filtered_headers]
    if removed:
        print("\n  [Removed Sections]")
        for sec in removed[:10]:
            print(f"    - {sec.header or '(no header)'} (zone: {sec.semantic_zone})")
        if len(removed) > 10:
            print(f"    ... and {len(removed) - 10} more removed")

    # ==================================================
    # STEP 9: Build Canonical Map
    # ==================================================
    print_header("STEP 9: CANONICAL MAP", "=")

    canonical_pipeline = CanonicalSectionPipeline()
    canonical_map = canonical_pipeline._build_canonical_map(filtered_sections)

    print(f"  Total Zones: {len(canonical_map)}")

    for zone, items in canonical_map.items():
        print_subheader(f"Zone: {zone} ({len(items)} items)")
        for item in items[:10]:
            truncated = item[:100] + "..." if len(item) > 100 else item
            print(f"    - {truncated}")
        if len(items) > 10:
            print(f"    ... and {len(items) - 10} more items")

    # ==================================================
    # FINAL SUMMARY
    # ==================================================
    print_header("FINAL SUMMARY", "#")

    print(f"  URL: {url}")
    print(f"  Fetch Method: {fetch_method}")
    print(f"  HTML Length: {len(html_content):,} chars")
    print(f"  Parsed Body Length: {len(body_text):,} chars")
    print(f"  Cleaned Lines: {len(cleaned_lines)}")
    print(f"  Raw Sections: {len(raw_sections)}")
    print(f"  Sections (after adapt): {len(sections)}")
    print(f"  Sections (after semantic): {len(semantic_sections)}")
    print(f"  Sections (after filter): {len(filtered_sections)}")
    print(f"  Canonical Zones: {list(canonical_map.keys())}")

    for zone, items in canonical_map.items():
        print(f"    - {zone}: {len(items)} items")

    print_header("DEBUG COMPLETE", "#")

    return canonical_map


def run_test_urls():
    """미리 정의된 테스트 URL 실행"""
    test_urls = [
        {
            "url": "https://recruit.navercorp.com/rcrt/view.do?annoId=30004428&sw=&subJobCdArr=1010001%2C1010002%2C1010003%2C1010004%2C1010005%2C1010006%2C1010007%2C1010008%2C1010009%2C1010020%2C1020001%2C1030001%2C1030002%2C1040001%2C1040002%2C1040003%2C1050001%2C1050002%2C1060001&sysCompanyCdArr=&empTypeCdArr=&entTypeCdArr=&workAreaCdArr=",
            "brand_name": "NAVER",
            "position_name": "네이버 채용공고",
            "expected": "정적 HTML",
        },
        {
            "url": "https://www.wanted.co.kr/wd/268315",
            "brand_name": "Wanted",
            "position_name": "원티드 채용공고",
            "expected": "동적 JS 렌더링 (Playwright)",
        },
    ]

    print("\n" + "#" * 80)
    print("# URL Pipeline Debug Test (NEW OCR-style Pipeline)")
    print("#" * 80)

    for i, test_case in enumerate(test_urls, 1):
        print(f"\n\n{'#' * 80}")
        print(f"# Test Case {i}: {test_case['expected']}")
        print(f"{'#' * 80}")

        try:
            test_url_pipeline_verbose(
                url=test_case["url"],
                brand_name=test_case["brand_name"],
                position_name=test_case["position_name"],
            )
        except Exception as e:
            print(f"\n[FAILED] {e}")
            import traceback
            traceback.print_exc()

        if i < len(test_urls):
            print("\n... waiting 2 seconds before next test ...")
            time.sleep(2)


# ==================================================
# Redis Consumer 모드
# ==================================================

def run_consumer_mode():
    """Redis Consumer로 메시지 읽어서 처리"""
    from infra.redis.redis_client import RedisClient
    from infra.redis.stream_consumer import RedisStreamConsumer
    from infra.redis.stream_keys import JdStreamKeys
    from preprocess.worker.redis.jd_preprocess_url_worker import JdPreprocessUrlWorker
    from inputs.parse_jd_preprocess_message import parse_jd_preprocess_message

    redis_client = RedisClient()

    consumer = RedisStreamConsumer(
        redis_client=redis_client,
        stream_key=JdStreamKeys.PREPROCESS_URL_REQUEST,
        group="jd-url-group",
        consumer_name="jd-url-consumer-1"
    )

    worker = JdPreprocessUrlWorker()

    print("\n" + "=" * 80)
    print("[CONSUMER MODE] Waiting for messages from Redis Stream...")
    print(f"  stream_key: {JdStreamKeys.PREPROCESS_URL_REQUEST}")
    print("=" * 80)

    messages = consumer.read()

    if not messages:
        print("\n[INFO] No messages in stream")
        return

    for msg in messages:
        entry_id = msg.get("id")

        input_dto = parse_jd_preprocess_message(msg)

        try:
            if input_dto.source != "URL":
                raise ValueError(f"URL worker received non-URL source: {input_dto.source}")

            if not input_dto.url:
                raise ValueError("URL worker received empty url")

            print(f"\n[PROCESSING] entry_id={entry_id} url={input_dto.url}")

            output = worker.process(input_dto)

            consumer.ack(entry_id)

            print(f"[SUCCESS] ACKed entry_id={entry_id}")

        except Exception as e:
            logger.error(
                "[JD_URL_PREPROCESS_ABORTED] requestId=%s entryId=%s errorType=%s errorMessage=%s",
                getattr(input_dto, "request_id", None),
                entry_id,
                type(e).__name__,
                str(e),
            )


# ==================================================
# Main
# ==================================================

def main():
    parser = argparse.ArgumentParser(
        description="URL Pipeline 디버그 스크립트 (OCR 방식 새 파이프라인)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
사용 예시:
  # URL 직접 테스트 - 모든 처리 단계 출력
  python scripts/debug_url_summary.py --url "https://example.com/job"

  # 테스트 URL 2개 자동 실행
  python scripts/debug_url_summary.py --test

  # Redis Consumer 모드 (Redis 필요)
  python scripts/debug_url_summary.py --consumer
        """
    )

    parser.add_argument(
        "--url",
        type=str,
        help="테스트할 URL (Redis 불필요)"
    )
    parser.add_argument(
        "--test",
        action="store_true",
        help="미리 정의된 테스트 URL 2개 실행"
    )
    parser.add_argument(
        "--consumer",
        action="store_true",
        help="Redis Consumer 모드로 실행"
    )
    parser.add_argument(
        "--brand",
        type=str,
        default="TestBrand",
        help="브랜드명 (--url과 함께 사용)"
    )
    parser.add_argument(
        "--position",
        type=str,
        default="TestPosition",
        help="포지션명 (--url과 함께 사용)"
    )

    args = parser.parse_args()

    if args.url:
        test_url_pipeline_verbose(
            url=args.url,
            brand_name=args.brand,
            position_name=args.position,
        )
    elif args.test:
        run_test_urls()
    elif args.consumer:
        run_consumer_mode()
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
