import logging
import time
from urllib.parse import parse_qs, urlencode, urlparse, urlunparse
from typing import Optional
from urllib.parse import urljoin

from bs4 import BeautifulSoup
from playwright.sync_api import TimeoutError as PlaywrightTimeoutError

from url.fetcher import UrlFetcher
from url.playwright_fetcher import PlaywrightFetcher

logger = logging.getLogger(__name__)


class SaraminPlaywrightFetcher(PlaywrightFetcher):
    """Saramin-specific Playwright fetcher."""
    def __init__(self, timeout: int = 60000, headless: bool = True):
        super().__init__(timeout=timeout, headless=headless)
        # Keep static timeout shorter than Playwright timeout.
        self.static_fetcher = UrlFetcher(timeout=max(10, min(20, int(timeout / 1000))))

    def fetch(self, url: str) -> str:
        target_url = self._to_view_detail_url(url)
        logger.debug("Saramin fetch target normalized", extra={"input_url": url, "target_url": target_url})

        # 0) Static chain first:
        #    view HTML -> iframe src(view-detail) -> fetch detail HTML
        # This path is resilient when Playwright is blocked by network/policy.
        static_html = self._fetch_via_static_chain(url, target_url)
        if static_html and self._looks_like_jd_html(static_html):
            logger.info(
                "Saramin static chain fetch succeeded",
                extra={"url": url, "target_url": target_url, "html_length": len(static_html)},
            )
            return static_html

        if not self.is_available():
            if static_html:
                return static_html
            return self.static_fetcher.fetch(target_url)

        from playwright.sync_api import sync_playwright

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
                        viewport={"width": 1920, "height": 1080},
                    )
                    page = context.new_page()

                    # 0) If target is detail URL, warm up session on original view URL first.
                    if target_url != url:
                        try:
                            self._navigate_with_fallback(page, url, timeout_ms=min(self.timeout, 20000))
                        except Exception:
                            # best-effort warm-up only
                            pass

                    # 1) Try detail URL first, then fallback to original view URL.
                    candidates = [target_url]
                    if target_url != url:
                        candidates.append(url)

                    last_error: Optional[Exception] = None
                    navigated = False
                    for candidate in candidates:
                        try:
                            self._navigate_with_fallback(
                                page,
                                candidate,
                                timeout_ms=self.timeout,
                                referer=url if candidate == target_url and target_url != url else None,
                            )
                            navigated = True
                            break
                        except Exception as nav_error:
                            last_error = nav_error
                            logger.warning(
                                "Saramin navigation candidate failed",
                                extra={"candidate_url": candidate, "error": str(nav_error)},
                            )
                            continue

                    if not navigated:
                        raise last_error or RuntimeError("Saramin navigation failed")

                    time.sleep(2)
                    logger.debug("Saramin page navigated", extra={"input_url": url, "target_url": target_url, "final_page_url": page.url})
                    try:
                        page.wait_for_load_state("networkidle", timeout=10000)
                    except Exception:
                        pass
                    time.sleep(0.8)

                    frame_html = self._extract_primary_saramin_frame_html(page)
                    if frame_html:
                        logger.info(
                            "Saramin Playwright frame fetch succeeded",
                            extra={"url": target_url, "html_length": len(frame_html)},
                        )
                        return frame_html

                    self._scroll_page(page)
                    self._click_expand_buttons(page)
                    time.sleep(0.4)

                    html = page.content()
                    logger.info(
                        "Saramin Playwright fallback page fetch succeeded",
                        extra={"url": target_url, "html_length": len(html)},
                    )
                    return html
                finally:
                    browser.close()
        except Exception as e:
            logger.error("Saramin Playwright fetch failed", extra={"url": target_url, "error": str(e)})
            if static_html:
                logger.warning(
                    "Saramin fallback to static chain HTML after Playwright failure",
                    extra={"url": url, "target_url": target_url, "html_length": len(static_html)},
                )
                return static_html
            retry_static = self._fetch_via_static_chain(url, target_url)
            if retry_static:
                logger.warning(
                    "Saramin fallback to retried static chain HTML after Playwright failure",
                    extra={"url": url, "target_url": target_url, "html_length": len(retry_static)},
                )
                return retry_static
            raise

    def _navigate_with_fallback(self, page, target_url: str, timeout_ms: int, referer: Optional[str] = None) -> None:
        """Navigate robustly for Saramin. Prefer commit to avoid domcontentloaded hangs."""
        if referer:
            page.set_extra_http_headers({"Referer": referer})
        try:
            page.goto(target_url, wait_until="commit", timeout=min(timeout_ms, 15000))
        except PlaywrightTimeoutError:
            logger.warning(
                "Saramin commit timeout, retrying with domcontentloaded",
                extra={"url": target_url, "timeout_ms": timeout_ms},
            )
            page.goto(target_url, wait_until="domcontentloaded", timeout=timeout_ms)

        try:
            page.wait_for_load_state("domcontentloaded", timeout=5000)
        except Exception:
            pass

        try:
            self._wait_for_saramin_content(page, timeout_ms=8000)
        except Exception:
            pass

        try:
            page.wait_for_load_state("networkidle", timeout=6000)
        except Exception:
            pass

    def _wait_for_saramin_content(self, page, timeout_ms: int = 8000) -> None:
        """Wait until JD-like content appears either on main page or iframe."""
        deadline = time.time() + (timeout_ms / 1000.0)
        while time.time() < deadline:
            try:
                # Main DOM content candidates
                for selector in (".jv_cont", ".wrap_jv_cont", ".recruit_content", ".user_content"):
                    loc = page.locator(selector)
                    if loc.count() > 0:
                        text = loc.first.inner_text(timeout=800)
                        if text and len(text.strip()) > 120:
                            return
            except Exception:
                pass

            try:
                # Iframe content candidates
                for frame in page.frames:
                    if frame == page.main_frame:
                        continue
                    name = (frame.name or "").lower()
                    frame_url = (frame.url or "").lower()
                    if "iframe_content" not in name and "view-detail" not in frame_url:
                        continue
                    body = frame.locator("body")
                    if body.count() == 0:
                        continue
                    text = body.first.inner_text(timeout=800)
                    if text and len(text.strip()) > 120:
                        return
            except Exception:
                pass

            time.sleep(0.25)

    def _to_view_detail_url(self, url: str) -> str:
        """Normalize Saramin relay/view URL to relay/view-detail URL when possible."""
        parsed = urlparse(url)
        if "saramin.co.kr" not in (parsed.netloc or "").lower():
            return url

        if "/relay/view-detail" in parsed.path:
            return url

        qs = parse_qs(parsed.query or "")
        rec_idx = (qs.get("rec_idx") or [None])[0]
        if not rec_idx:
            return url

        # Keep existing query params as much as possible.
        # Dropping tracking/context params can lead Saramin to return shell-only pages.
        detail_qs = {k: v[0] for k, v in qs.items() if v}
        detail_qs["rec_idx"] = rec_idx

        # rec_seq priority:
        # 1) existing query rec_seq
        # 2) fragment seq (e.g. #seq=0 from relay/view URL)
        # 3) default 0
        rec_seq = detail_qs.get("rec_seq")
        if not rec_seq:
            frag_qs = parse_qs(parsed.fragment or "")
            rec_seq = (frag_qs.get("seq") or ["0"])[0]
        detail_qs["rec_seq"] = rec_seq or "0"

        # relay/view-only query can be removed in detail URL
        detail_qs.pop("view_type", None)

        rebuilt = parsed._replace(
            path="/zf_user/jobs/relay/view-detail",
            query=urlencode(detail_qs),
            fragment="",
        )
        return urlunparse(rebuilt)

    def _extract_primary_saramin_frame_html(self, page) -> str:
        """Prefer frame HTML when Saramin detail is rendered inside iframe."""
        try:
            for frame in page.frames:
                name = (frame.name or "").lower()
                frame_url = (frame.url or "").lower()
                if (
                    "iframe_content" in name
                    or "view-detail" in frame_url
                    or "saramin.co.kr" in frame_url and frame != page.main_frame
                ):
                    try:
                        body_exists = frame.locator("body").count() > 0
                    except Exception:
                        body_exists = False
                    if not body_exists:
                        continue
                    html = frame.content()
                    if html and len(html) > 300:
                        return html
        except Exception as e:
            logger.debug("Saramin frame extraction failed", extra={"error": str(e)})
        return ""

    def _fetch_via_static_chain(self, input_url: str, target_url: str) -> str:
        """Fetch Saramin detail HTML via static requests (view -> iframe src -> detail)."""
        candidates = [input_url]
        if target_url != input_url:
            candidates.append(target_url)

        for candidate in candidates:
            try:
                html = self.static_fetcher.fetch(candidate)
            except Exception as e:
                logger.debug("Saramin static candidate fetch failed", extra={"candidate_url": candidate, "error": str(e)})
                continue

            # Directly good enough.
            if self._looks_like_jd_html(html):
                return html

            # If shell page, try iframe src extraction.
            iframe_src = self._extract_iframe_src(candidate, html)
            if not iframe_src:
                continue
            try:
                iframe_html = self.static_fetcher.fetch(iframe_src)
                if iframe_html:
                    return iframe_html
            except Exception as e:
                logger.debug("Saramin iframe src fetch failed", extra={"iframe_src": iframe_src, "error": str(e)})
                continue

        return ""

    def _extract_iframe_src(self, base_url: str, html: str) -> str:
        """Extract best iframe src that likely contains Saramin JD detail."""
        if not html:
            return ""
        try:
            soup = BeautifulSoup(html, "html.parser")
            iframes = soup.find_all("iframe")
            for iframe in iframes:
                src = (iframe.get("src") or "").strip()
                if not src:
                    continue
                low = src.lower()
                if "view-detail" in low or "iframe_content" in (iframe.get("id") or "").lower():
                    resolved = urljoin(base_url, src)
                    logger.debug("Saramin iframe src extracted", extra={"base_url": base_url, "iframe_src": resolved})
                    return resolved
            # Fallback: first iframe with saramin host
            for iframe in iframes:
                src = (iframe.get("src") or "").strip()
                if not src:
                    continue
                resolved = urljoin(base_url, src)
                if "saramin.co.kr" in resolved.lower():
                    logger.debug("Saramin iframe fallback src extracted", extra={"base_url": base_url, "iframe_src": resolved})
                    return resolved
        except Exception as e:
            logger.debug("Saramin iframe src extraction failed", extra={"error": str(e)})
        return ""

    @staticmethod
    def _looks_like_jd_html(html: str) -> bool:
        if not html:
            return False
        low = html.lower()
        markers = [
            "jv_cont",
            "recruit_content",
            "모집부문",
            "자격요건",
            "지원자격",
            "담당업무",
            "우대사항",
            "requirements",
            "responsibilities",
            "qualifications",
        ]
        return any(m in low for m in markers)
