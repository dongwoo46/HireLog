# src/url/playwright_fetcher.py

import logging
import time
from typing import Optional, List

logger = logging.getLogger(__name__)

# Playwright 설치 여부 확인
_PLAYWRIGHT_AVAILABLE = False
try:
    from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError
    _PLAYWRIGHT_AVAILABLE = True
except ImportError:
    logger.warning(
        "Playwright not installed. JS rendering will not be available. "
        "Install with: pip install playwright && playwright install chromium"
    )


# "더보기" 버튼 텍스트 패턴 (한국어 + 영어)
EXPAND_BUTTON_TEXTS = [
    # 한국어 (Wanted 등)
    "상세 정보 더 보기",
    "상세정보 더보기",
    "상세정보 더 보기",
    "더보기",
    "더 보기",
    "자세히 보기",
    "자세히보기",
    "펼치기",
    "전체보기",
    "전체 보기",
    "내용 더보기",
    "내용 더 보기",
    "접기",  # 이미 펼쳐진 상태일 수 있음
    # 영어
    "Show more",
    "View more",
    "Read more",
    "See more",
    "Expand",
    "Load more",
    "See full description",
    "View full description",
]


class PlaywrightFetcher:
    """
    Playwright 기반 동적 페이지 Fetcher

    책임:
    - JavaScript 렌더링이 필요한 페이지 fetch
    - "더보기" 버튼 자동 클릭으로 숨겨진 콘텐츠 로드
    - headless 브라우저로 페이지 로드 후 HTML 반환

    비책임:
    - HTML 파싱 / 의미 추출 ❌
    - 렌더링 필요 여부 판단 ❌ (UrlFetcher에서 결정)
    """

    def __init__(self, timeout: int = 60000, headless: bool = True):
        """
        Args:
            timeout: 페이지 로드 타임아웃 (밀리초), 기본 60초
            headless: headless 모드 여부
        """
        self.timeout = timeout
        self.headless = headless

    def is_available(self) -> bool:
        """Playwright 사용 가능 여부 반환"""
        return _PLAYWRIGHT_AVAILABLE

    def fetch(self, url: str) -> str:
        """
        URL에서 JavaScript 렌더링된 HTML을 반환한다.
        "더보기" 버튼이 있으면 자동으로 클릭하여 전체 콘텐츠를 로드한다.

        Args:
            url: 대상 URL

        Returns:
            렌더링된 HTML 문자열

        Raises:
            RuntimeError: Playwright 미설치 시
            Exception: 브라우저 오류
        """
        if not _PLAYWRIGHT_AVAILABLE:
            raise RuntimeError(
                "Playwright is not installed. "
                "Install with: pip install playwright && playwright install chromium"
            )

        try:
            with sync_playwright() as p:
                browser = p.chromium.launch(headless=self.headless)

                try:
                    context = browser.new_context(
                        user_agent=(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            "AppleWebKit/537.36 (KHTML, like Gecko) "
                            "Chrome/120.0.0.0 Safari/537.36"
                        ),
                        viewport={"width": 1920, "height": 1080}
                    )
                    page = context.new_page()

                    # 페이지 로드 (domcontentloaded로 먼저 대기)
                    page.goto(url, wait_until="domcontentloaded", timeout=self.timeout)

                    # 추가 대기 (동적 콘텐츠 로드 완료 대기)
                    time.sleep(2)  # JS 렌더링 대기

                    # networkidle 대기 시도 (최대 10초)
                    try:
                        page.wait_for_load_state("networkidle", timeout=10000)
                    except:
                        pass  # 타임아웃 무시

                    time.sleep(1)  # 추가 대기

                    # 페이지 스크롤 (lazy load 콘텐츠 로드)
                    self._scroll_page(page)

                    # "더보기" 버튼 클릭 (숨겨진 콘텐츠 로드)
                    self._click_expand_buttons(page)

                    # 최종 대기
                    time.sleep(0.5)

                    # 렌더링된 HTML 반환
                    html = page.content()

                    logger.info(
                        "[PLAYWRIGHT_FETCH_SUCCESS] url=%s length=%d",
                        url, len(html)
                    )

                    return html

                finally:
                    browser.close()

        except Exception as e:
            logger.error(
                "[PLAYWRIGHT_FETCH_FAILED] url=%s error=%s",
                url, str(e)
            )
            raise

    def _scroll_page(self, page) -> None:
        """페이지를 스크롤하여 lazy load 콘텐츠를 로드한다."""
        try:
            # 페이지 끝까지 스크롤
            page.evaluate("""
                () => {
                    window.scrollTo(0, document.body.scrollHeight / 2);
                }
            """)
            time.sleep(0.3)

            page.evaluate("""
                () => {
                    window.scrollTo(0, document.body.scrollHeight);
                }
            """)
            time.sleep(0.3)

            # 다시 위로 스크롤 (버튼이 위에 있을 수 있음)
            page.evaluate("""
                () => {
                    window.scrollTo(0, 0);
                }
            """)
            time.sleep(0.3)

        except Exception as e:
            logger.debug(f"[SCROLL_FAILED] error={e}")

    def _click_expand_buttons(self, page) -> int:
        """
        페이지에서 "더보기" 버튼들을 찾아 클릭한다.

        Returns:
            클릭한 버튼 개수
        """
        clicked_count = 0

        # 방법 1: getByText로 정확한 텍스트 매칭
        for text in EXPAND_BUTTON_TEXTS:
            try:
                # 정확한 텍스트 매칭
                locator = page.get_by_text(text, exact=False)
                count = locator.count()

                if count > 0:
                    for i in range(count):
                        try:
                            element = locator.nth(i)
                            if element.is_visible():
                                # 요소로 스크롤
                                element.scroll_into_view_if_needed()
                                time.sleep(0.2)

                                # 클릭
                                element.click(timeout=3000)
                                clicked_count += 1

                                logger.info(
                                    "[EXPAND_BUTTON_CLICKED] text='%s' index=%d",
                                    text, i
                                )

                                # 클릭 후 DOM 업데이트 대기
                                time.sleep(1)
                                try:
                                    page.wait_for_load_state("networkidle", timeout=3000)
                                except:
                                    pass

                                # 한 번 클릭하면 다음 패턴으로 (중복 클릭 방지)
                                break

                        except Exception as click_error:
                            logger.debug(
                                "[EXPAND_CLICK_FAILED] text='%s' error=%s",
                                text, str(click_error)
                            )
                            continue

            except Exception as e:
                logger.debug(
                    "[EXPAND_SEARCH_FAILED] text='%s' error=%s",
                    text, str(e)
                )
                continue

        # 방법 2: JavaScript로 직접 클릭 (fallback)
        if clicked_count == 0:
            clicked_count += self._click_expand_buttons_js(page)

        if clicked_count > 0:
            logger.info("[EXPAND_BUTTONS_TOTAL] clicked=%d", clicked_count)

        return clicked_count

    def _click_expand_buttons_js(self, page) -> int:
        """JavaScript로 더보기 버튼을 찾아 클릭한다."""
        try:
            result = page.evaluate("""
                () => {
                    const patterns = [
                        '상세 정보 더 보기', '상세정보 더보기', '더보기', '더 보기',
                        'Show more', 'View more', 'Read more', 'See more', 'Expand'
                    ];

                    let clicked = 0;

                    // 모든 클릭 가능한 요소 검색
                    const elements = document.querySelectorAll('button, a, span, div, [role="button"]');

                    for (const el of elements) {
                        const text = el.textContent?.trim() || '';

                        for (const pattern of patterns) {
                            if (text.includes(pattern) || text === pattern) {
                                // 요소가 보이는지 확인
                                const rect = el.getBoundingClientRect();
                                if (rect.width > 0 && rect.height > 0) {
                                    try {
                                        el.click();
                                        clicked++;
                                        console.log('[JS_CLICK]', pattern, el.tagName);
                                        break;
                                    } catch (e) {
                                        console.log('[JS_CLICK_ERROR]', e);
                                    }
                                }
                            }
                        }
                    }

                    return clicked;
                }
            """)

            if result > 0:
                logger.info("[JS_EXPAND_CLICKED] count=%d", result)
                time.sleep(1)
                try:
                    page.wait_for_load_state("networkidle", timeout=3000)
                except:
                    pass

            return result

        except Exception as e:
            logger.debug(f"[JS_EXPAND_FAILED] error={e}")
            return 0
