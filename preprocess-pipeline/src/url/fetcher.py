import os
import requests
import logging
from typing import Optional

logger = logging.getLogger(__name__)

# 기본 timeout (환경 변수로 오버라이드 가능)
DEFAULT_URL_FETCH_TIMEOUT = 10


class UrlFetcher:
    """
    URL Content Fetcher

    책임:
    - URL 요청 및 HTML 텍스트 반환
    - 정적(Requests) vs 동적(JS) 분기 처리 (기반 마련)

    설정:
    - timeout: 환경 변수 URL_FETCH_TIMEOUT 또는 생성자 파라미터
    """

    def __init__(self, timeout: int | None = None):
        self.timeout = timeout or int(
            os.environ.get("URL_FETCH_TIMEOUT", str(DEFAULT_URL_FETCH_TIMEOUT))
        )
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

    def fetch(self, url: str) -> str:
        """
        URL 내용을 가져온다.
        
        TODO: JS 렌더링이 필요한 경우에 대한 분기 처리를 고도화해야 함.
        현재는 정적 요청 시도 후 실패하거나 특정 조건에서 JS 렌더링(미구현)을 고려하는 구조.
        """
        try:
            response = requests.get(url, headers=self.headers, timeout=self.timeout)
            response.raise_for_status()

            # requests는 charset 미지정 응답에 ISO-8859-1을 기본 적용할 수 있어
            # 한글 페이지가 깨질 수 있다.
            # 1) Content-Type charset 확인
            # 2) 없거나 latin-1이면 raw bytes로 UTF-8 → EUC-KR 순으로 직접 디코딩
            declared_encoding = (response.encoding or "").lower()
            if (
                not declared_encoding
                or declared_encoding in {"iso-8859-1", "latin-1"}
                or "8859-1" in declared_encoding
            ):
                raw = response.content
                for enc in ("utf-8", "euc-kr", "cp949"):
                    try:
                        html_content = raw.decode(enc)
                        break
                    except (UnicodeDecodeError, LookupError):
                        continue
                else:
                    html_content = raw.decode("utf-8", errors="replace")
            else:
                html_content = response.text
            
            # JS 렌더링 필요 여부 판단 (단순 휴리스틱)
            if self._needs_js_rendering(html_content):
                logger.debug("JS rendering may be required", extra={"url": url})
                # 여기서 Playwright/Selenium 등을 호출하거나 외부 서비스로 넘기는 로직 추가 가능

            return html_content

        except requests.RequestException as e:
            logger.error("URL fetch failed", extra={"url": url, "error": str(e)})
            raise

    def _needs_js_rendering(self, html: str) -> bool:
        """
        HTML 내용을 분석하여 JS 렌더링이 필요한지 판단한다. (Hybrid Approach)
        
        조건:
        1. HTML 바디 길이가 너무 짧음 (내용 없음)
        2. 특정 SPA Root 요소만 존재
        3. JD 핵심 키워드(Responsibilities, Requirements 등) 부재 시 의심
        
        Returns:
            bool: True if JS rendering is likely required.
        """
        if not html:
            return True

        # 1. 길이 체크 (너무 짧으면 의심)
        if len(html) < 500:
            logger.debug("JS rendering required: HTML too short", extra={"html_length": len(html)})
            return True

        # 2. SPA Root Indicators
        spa_indicators = [
            '<div id="app"></div>',
            '<div id="root"></div>',
            '<div id="__next"></div>',
            '<body></body>', # Empty Body
            'You need to enable JavaScript to run this app'
        ]

        for indicator in spa_indicators:
            if indicator in html:
                logger.debug("JS rendering required: SPA indicator found", extra={"indicator": indicator})
                return True

        # 3. "더보기" 버튼 존재 체크 (숨겨진 콘텐츠 = 클릭 필요 = Playwright 필요)
        expand_button_patterns = [
            "상세 정보 더 보기", "상세정보 더보기", "상세정보 더 보기",
            "더보기", "더 보기", "자세히 보기", "자세히보기",
            "펼치기", "전체보기", "전체 보기", "내용 더보기", "내용 더 보기",
            "Show more", "View more", "Read more", "See more", "Expand",
            "Load more", "See full description", "View full description",
        ]

        for pattern in expand_button_patterns:
            if pattern in html:
                logger.debug("JS rendering required: expand button found", extra={"pattern": pattern})
                return True

        # 4. 핵심 키워드 체크 (JD 본문이 로드되었는지 확인)
        # 통상적인 JD에 있는 단어들이 하나도 없다면 렌더링 안된 것으로 간주
        # (영어/한글 주요 키워드)
        keywords = [
            # KR
            "자격요건", "우대사항", "담당업무", "주요업무", "지원자격", "복리후생", "채용절차", "전형절차", "기술스택",
            # EN
            "Requirements", "Responsibilities", "Qualifications", "Preferred", "Description", "Benefits", "About the role"
        ]

        found_keywords = [k for k in keywords if k in html]
        if not found_keywords:
            # 키워드가 하나도 없다고 무조건 JS문제는 아니지만(이미지 통짜일수도),
            # 텍스트 파이프라인 관점에서는 '텍스트 없음'으로 간주하고 렌더링 시도해보는 게 맞음.
            logger.debug("JS rendering required: no JD keywords found")
            return True

        return False
