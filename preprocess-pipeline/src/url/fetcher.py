import requests
import logging
from typing import Optional

logger = logging.getLogger(__name__)

class UrlFetcher:
    """
    URL Content Fetcher
    
    책임:
    - URL 요청 및 HTML 텍스트 반환
    - 정적(Requests) vs 동적(JS) 분기 처리 (기반 마련)
    """

    def __init__(self, timeout: int = 10):
        self.timeout = timeout
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
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
            
            # 인코딩 자동 감지
            if response.encoding is None:
                response.encoding = response.apparent_encoding
                
            html_content = response.text
            
            # JS 렌더링 필요 여부 판단 (단순 휴리스틱)
            if self._needs_js_rendering(html_content):
                logger.warning(f"JS rendering might be required for {url} (Not implemented yet, returning raw HTML)")
                # 여기서 Playwright/Selenium 등을 호출하거나 외부 서비스로 넘기는 로직 추가 가능
                
            return html_content
            
        except requests.RequestException as e:
            logger.error(f"Failed to fetch content from {url}: {e}")
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
            logger.info(f"JS Rendering Required: HTML length too short ({len(html)})")
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
                 logger.info(f"JS Rendering Required: SPA indicator found '{indicator}'")
                 return True

        # 3. 핵심 키워드 체크 (JD 본문이 로드되었는지 확인)
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
            logger.info("JS Rendering Required: No JD keywords found in HTML")
            return True
                
        return False
