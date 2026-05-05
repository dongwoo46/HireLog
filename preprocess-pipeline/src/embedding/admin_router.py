# src/embedding/admin_router.py

"""
Admin URL Fetch Router

역할:
- Spring Admin 경로에서 동기 호출하는 HTTP 엔드포인트
- URL 크롤링 + 노이즈 제거 후 텍스트 반환 (섹션 분리 스킵)
- 텍스트 추출 실패 시 JD 영역 이미지를 base64로 반환 (Gemini 멀티모달 처리용)

플랫폼별 fetch 전략:
  JOBKOREA → requests (정적 HTML)
  SARAMIN  → SaraminPlaywrightFetcher (정적 체인 우선, Playwright fallback)
  그 외    → PlaywrightFetcher (Wanted, Remember 등)
"""

import base64
import logging
from typing import Optional
from urllib.parse import urljoin

import requests as http_requests
from bs4 import BeautifulSoup
from fastapi import APIRouter
from pydantic import BaseModel

from domain.job_platform import JobPlatform
from url.fetcher import UrlFetcher
from url.fetchers.saramin_playwright_fetcher import SaraminPlaywrightFetcher
from url.parsers.jobkorea_parser import JobKoreaUrlParser
from url.parsers.parser import UrlParser
from url.parsers.saramin_parser import SaraminUrlParser
from url.playwright_fetcher import PlaywrightFetcher
from url.preprocessor import preprocess_url_text

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/admin", tags=["admin"])

# 텍스트가 충분하다고 판단하는 최소 길이
MIN_TEXT_LENGTH = 200

# 이미지 최소 크기 (이 미만은 아이콘/트래킹 픽셀로 간주)
MIN_IMAGE_BYTES = 5_000

# 최대 추출 이미지 수
MAX_IMAGES = 10

# JD 콘텐츠 루트 셀렉터 (플랫폼별 우선순위 순)
JD_ROOT_SELECTORS = [
    ".jv_cont", ".wrap_jv_cont", ".recruit_content", ".user_content",  # Saramin
    ".tplJobView", ".recruitment-detail", ".recruit-content",          # JobKorea
    "[class*='JobDescription']", "[class*='job-detail']",              # Wanted
    "article", "main", "#content", "#container", "#contents",
]

# 무시할 이미지 URL 패턴
IGNORE_IMAGE_PATTERNS = [
    ".svg", "icon", "logo", "1x1", "pixel", "tracking", "beacon", "blank", "spinner",
]


# ─── Request / Response ──────────────────────────────────────────────────────

class AdminFetchUrlRequest(BaseModel):
    url: str


class AdminFetchUrlResponse(BaseModel):
    """
    status:
      SUCCESS       텍스트 추출 성공
      IMAGE_BASED   이미지 기반 JD — images 필드에 base64 데이터 포함
      INSUFFICIENT  텍스트도 이미지도 충분하지 않음 (수동 입력 필요)
      ERROR         크롤링/파싱 실패
    """
    status: str
    text: Optional[str] = None
    images: Optional[list[str]] = None  # "data:{mime};base64,{data}" 형식
    message: Optional[str] = None


# ─── 엔드포인트 ─────────────────────────────────────────────────────────────

@router.post("/fetch-url", response_model=AdminFetchUrlResponse)
def fetch_url(req: AdminFetchUrlRequest) -> AdminFetchUrlResponse:
    platform = JobPlatform.from_url(req.url)
    logger.info("[ADMIN_FETCH_URL_START] url=%s, platform=%s", req.url, platform)

    try:
        html = _fetch_html(req.url, platform)
        raw_text = _parse_text(html, platform, req.url)
        cleaned_lines = preprocess_url_text(raw_text, platform)
        cleaned_text = "\n".join(cleaned_lines).strip()

        if len(cleaned_text) >= MIN_TEXT_LENGTH:
            logger.info("[ADMIN_FETCH_URL_SUCCESS] url=%s, text_len=%d", req.url, len(cleaned_text))
            return AdminFetchUrlResponse(status="SUCCESS", text=cleaned_text)

        # 텍스트 부족 → 이미지 추출 시도
        logger.info(
            "[ADMIN_FETCH_URL_TEXT_SHORT] url=%s, text_len=%d, trying images",
            req.url, len(cleaned_text),
        )
        images = _extract_jd_images(html, req.url)

        if images:
            logger.info("[ADMIN_FETCH_URL_IMAGE_BASED] url=%s, image_count=%d", req.url, len(images))
            return AdminFetchUrlResponse(
                status="IMAGE_BASED",
                text=cleaned_text if cleaned_text else None,
                images=images,
            )

        logger.warning("[ADMIN_FETCH_URL_INSUFFICIENT] url=%s", req.url)
        return AdminFetchUrlResponse(
            status="INSUFFICIENT",
            message=f"텍스트와 이미지 모두 추출 실패. JD 텍스트를 직접 입력해주세요. (추출된 텍스트: {len(cleaned_text)}자)",
        )

    except Exception as e:
        logger.error("[ADMIN_FETCH_URL_ERROR] url=%s, error=%s", req.url, str(e), exc_info=True)
        return AdminFetchUrlResponse(status="ERROR", message=str(e))


# ─── 내부 헬퍼 ──────────────────────────────────────────────────────────────

def _fetch_html(url: str, platform: JobPlatform) -> str:
    if platform == JobPlatform.JOBKOREA:
        return UrlFetcher().fetch(url)
    elif platform == JobPlatform.SARAMIN:
        return SaraminPlaywrightFetcher().fetch(url)
    else:
        # WANTED, REMEMBER, 기타 → Playwright
        return PlaywrightFetcher().fetch(url)


def _parse_text(html: str, platform: JobPlatform, url: str) -> str:
    if platform == JobPlatform.SARAMIN:
        result = SaraminUrlParser().parse(html, url)
    elif platform == JobPlatform.JOBKOREA:
        result = JobKoreaUrlParser().parse(html, url)
    else:
        result = UrlParser().parse(html, url)
    return result.get("body", "")


def _extract_jd_images(html: str, base_url: str) -> list[str]:
    """JD 영역 내 이미지 추출 → base64 data URI 반환"""
    if not html:
        return []

    soup = BeautifulSoup(html, "html.parser")
    root = _pick_jd_root(soup)
    fetcher = UrlFetcher(timeout=10)
    images_b64: list[str] = []

    for img in root.find_all("img")[:30]:
        src = (
            img.get("src")
            or img.get("data-src")
            or img.get("data-original")
            or ""
        ).strip()
        if not src:
            continue

        abs_url = urljoin(base_url, src)
        if not abs_url.startswith(("http://", "https://")):
            continue
        if _should_ignore(abs_url):
            continue

        encoded = _download_encode(abs_url, fetcher)
        if encoded:
            images_b64.append(encoded)
            if len(images_b64) >= MAX_IMAGES:
                break

    return images_b64


def _pick_jd_root(soup: BeautifulSoup):
    best = None
    best_len = -1
    for selector in JD_ROOT_SELECTORS:
        try:
            node = soup.select_one(selector)
            if node:
                text_len = len(node.get_text(strip=True))
                if text_len > best_len:
                    best = node
                    best_len = text_len
        except Exception:
            continue
    return best if best is not None else (soup.body if soup.body else soup)


def _should_ignore(url: str) -> bool:
    low = url.lower()
    return any(pat in low for pat in IGNORE_IMAGE_PATTERNS)


def _download_encode(url: str, fetcher: UrlFetcher) -> Optional[str]:
    try:
        resp = http_requests.get(url, timeout=10, headers=fetcher.headers)
        if resp.status_code != 200 or len(resp.content) < MIN_IMAGE_BYTES:
            return None
        mime = resp.headers.get("content-type", "image/jpeg").split(";")[0].strip()
        if not mime.startswith("image/"):
            return None
        b64 = base64.b64encode(resp.content).decode("utf-8")
        return f"data:{mime};base64,{b64}"
    except Exception as e:
        logger.debug("[ADMIN_IMAGE_DOWNLOAD_FAILED] url=%s, error=%s", url, str(e))
        return None
