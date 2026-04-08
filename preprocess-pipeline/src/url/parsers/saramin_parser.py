import logging
import re
from typing import Dict, List

from bs4 import BeautifulSoup, Tag

from url.parsers.parser import UrlParser

logger = logging.getLogger(__name__)


class SaraminUrlParser(UrlParser):
    """Saramin URL parser with structure-preserving extraction."""

    _ROOT_SELECTORS = [
        ".job-detail-section",
        ".job-table",
        ".job-section",
        ".job-content",
        ".job-item",
        ".jv_cont",
        ".wrap_jv_cont",
        ".recruit_content",
        ".user_content",
        "#content",
    ]

    _NOISE_PATTERNS = [
        r"구직자 개인정보 보호",
        r"개인정보 보호하는 방법",
        r"SNS\s*채팅",
        r"특정 상품 구매",
        r"금전\s*입금",
        r"신뢰받는 채용 파트너",
        r"안전하고 투명한 채용 환경",
    ]

    def parse(self, html: str, url: str = "") -> Dict[str, str]:
        if not html:
            return {"title": "", "body": ""}

        soup = BeautifulSoup(html, "html.parser")
        title = soup.title.string.strip() if soup.title and soup.title.string else ""

        for tag in soup(["script", "style", "noscript", "svg", "path", "header", "footer", "nav"]):
            tag.decompose()

        root = self._pick_root(soup)
        structured_lines = self._extract_structured_lines(root)
        structured_lines = self._filter_noise(structured_lines)

        if len(structured_lines) < 8:
            body = super().parse(html, url=url).get("body", "")
        else:
            body = "\n".join(structured_lines)

        return {"title": title, "body": body}

    def _pick_root(self, soup: BeautifulSoup) -> Tag:
        best = None
        best_len = -1
        for selector in self._ROOT_SELECTORS:
            try:
                nodes = soup.select(selector)
            except Exception:
                continue
            for node in nodes[:5]:
                text_len = len(node.get_text(" ", strip=True))
                if text_len > best_len:
                    best = node
                    best_len = text_len
        return best if best is not None else (soup.body if soup.body else soup)

    def _extract_structured_lines(self, root: Tag) -> List[str]:
        lines: List[str] = []

        for element in root.find_all(["h1", "h2", "h3", "h4", "table", "ul", "ol", "p", "div"], recursive=True):
            if not isinstance(element, Tag):
                continue
            name = element.name.lower()

            if name in {"h1", "h2", "h3", "h4"}:
                heading = self._clean_text(element.get_text(" ", strip=True))
                if heading:
                    lines.append(f"## {heading}")
                continue

            if name == "table":
                lines.extend(self._flatten_table(element))
                continue

            if name in {"ul", "ol"}:
                for li in element.find_all("li", recursive=False):
                    item = self._clean_text(li.get_text(" ", strip=True))
                    if item:
                        lines.append(f"- {item}")
                continue

            if name in {"p", "div"}:
                if element.find(["table", "ul", "ol", "h1", "h2", "h3", "h4"]):
                    continue
                text = self._clean_text(element.get_text(" ", strip=True))
                if text and len(text) >= 3:
                    lines.append(text)

        return self._dedupe(lines)

    def _flatten_table(self, table: Tag) -> List[str]:
        out: List[str] = []
        rows = table.find_all("tr")
        if not rows:
            return out

        headers: List[str] = []
        first_cells = rows[0].find_all(["th", "td"])
        if first_cells and any(cell.name == "th" for cell in first_cells):
            headers = [self._clean_text(c.get_text(" ", strip=True)) for c in first_cells]

        start_idx = 1 if headers else 0
        for tr in rows[start_idx:]:
            cells = tr.find_all(["th", "td"])
            values = [self._clean_text(c.get_text(" ", strip=True)) for c in cells]
            values = [v for v in values if v]
            if not values:
                continue

            if headers and len(headers) == len(values):
                for h, v in zip(headers, values):
                    if h and v:
                        out.append(f"{h}: {v}")
            else:
                out.append(" | ".join(values))

        return out

    def _filter_noise(self, lines: List[str]) -> List[str]:
        result: List[str] = []
        for line in lines:
            if any(re.search(p, line, re.IGNORECASE) for p in self._NOISE_PATTERNS):
                continue
            result.append(line)
        return result

    @staticmethod
    def _clean_text(text: str) -> str:
        text = re.sub(r"\s+", " ", (text or "")).strip()
        text = re.sub(r"[|]{2,}", "|", text)

        # Recover common broken English tokens from table-rendered spans/line wraps.
        replacements = {
            "sc ript": "script",
            "fr amework": "framework",
            "re act": "react",
        }
        low = text.lower()
        for src, dst in replacements.items():
            if src in low:
                text = re.sub(src, dst, text, flags=re.IGNORECASE)
                low = text.lower()

        # Join overly split short alpha fragments (e.g., "ja va" -> "java") conservatively.
        text = re.sub(
            r"\b([a-z]{1,2})\s+([a-z]{3,})\b",
            lambda m: f"{m.group(1)}{m.group(2)}",
            text,
        )
        return text

    @staticmethod
    def _dedupe(lines: List[str]) -> List[str]:
        seen = set()
        out: List[str] = []
        for line in lines:
            key = line.lower().strip()
            if not key or key in seen:
                continue
            seen.add(key)
            out.append(line)
        return out
