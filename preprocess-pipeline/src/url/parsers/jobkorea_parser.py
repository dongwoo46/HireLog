import re
from typing import Dict, List

from bs4 import BeautifulSoup, Tag

from url.parsers.parser import UrlParser


class JobKoreaUrlParser(UrlParser):
    """JobKorea-specific parser focused on the core JD block."""

    _ROOT_SELECTORS = [
        "#container",
        "#contents",
        ".tplJobView",
        ".recruitment-detail",
        ".recruit-content",
        ".dev-wrap",
    ]

    _DROP_SELECTORS = [
        ".tplAside",
        ".aside",
        ".summary",
        ".career-talk",
        ".review",
        ".interview",
        ".recom",
        ".recommend",
        ".similar",
        ".ad",
        ".banner",
        ".tag",
        ".company-info",
        ".map",
        ".location-map",
        ".jobs-relate",
    ]

    _NOISE_PATTERNS = [
        r"잡코리아.*무단전재",
        r"채용정보에 잘못된 내용이 있을 경우",
        r"불법/허위/과장/오류 신고",
        r"이 기업의 취업 전략",
        r"인적성.?면접 후기",
        r"관련 태그",
        r"추천.?공고",
        r"AI추천공고",
        r"즉시 지원",
    ]

    def parse(self, html: str, url: str = "") -> Dict[str, str]:
        if not html:
            return {"title": "", "body": ""}

        soup = BeautifulSoup(html, "html.parser")
        title = soup.title.string.strip() if soup.title and soup.title.string else ""

        for tag in soup(["script", "style", "noscript", "svg", "path", "header", "footer", "nav", "iframe"]):
            tag.decompose()

        for sel in self._DROP_SELECTORS:
            try:
                for node in soup.select(sel):
                    node.decompose()
            except Exception:
                continue

        root = self._pick_root(soup)
        lines = self._extract_structured_lines(root)
        lines = self._filter_noise(lines)

        if len(lines) < 8:
            body = super().parse(html, url=url).get("body", "")
        else:
            body = "\n".join(lines)

        return {"title": title, "body": body}

    def _pick_root(self, soup: BeautifulSoup) -> Tag:
        best = None
        best_len = -1
        for selector in self._ROOT_SELECTORS:
            try:
                nodes = soup.select(selector)
            except Exception:
                continue
            for node in nodes[:6]:
                text_len = len(node.get_text(" ", strip=True))
                if text_len > best_len:
                    best = node
                    best_len = text_len
        return best if best is not None else (soup.body if soup.body else soup)

    def _extract_structured_lines(self, root: Tag) -> List[str]:
        lines: List[str] = []
        for element in root.find_all(["h1", "h2", "h3", "h4", "strong", "dt", "th", "table", "ul", "ol", "li", "p", "div"], recursive=True):
            if not isinstance(element, Tag):
                continue
            name = element.name.lower()

            if name in {"h1", "h2", "h3", "h4", "strong", "dt", "th"}:
                heading = self._clean_text(element.get_text(" ", strip=True))
                if heading and self._looks_like_heading(heading):
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

            if name == "li":
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

        for tr in rows:
            th = tr.find("th")
            td = tr.find("td")
            if th and td:
                h = self._clean_text(th.get_text(" ", strip=True))
                v = self._clean_text(td.get_text(" ", strip=True))
                if h and v:
                    out.append(f"{h}: {v}")
                continue

            cells = tr.find_all(["th", "td"])
            values = [self._clean_text(c.get_text(" ", strip=True)) for c in cells]
            values = [v for v in values if v]
            if values:
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
        return text

    @staticmethod
    def _looks_like_heading(text: str) -> bool:
        if len(text) > 40:
            return False
        kw = (
            "모집", "업무", "자격", "우대", "근무", "복지", "절차", "지원", "전형",
            "responsibilities", "requirements", "preferred", "qualification",
        )
        low = text.lower()
        return any(k in low for k in kw)

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

