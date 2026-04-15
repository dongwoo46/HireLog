from collections import defaultdict
import re
from preprocess.semantic.semantic_preprocessor import apply_semantic_lite
from preprocess.semantic.section_filter import filter_irrelevant_sections


class CanonicalSectionPipeline:
    """
    구조화된 Section 목록을 입력으로 받아
    semantic-lite → filter → canonical_map 을 생성한다.

    책임:
    - 이미 '의미 단위로 구조화된 sections'만 처리
    - Core / Structural 단계에는 관여하지 않는다
    - TEXT / OCR / URL 파이프라인에서 공통으로 재사용된다
    """

    def process(self, sections) -> dict[str, list[str]]:
        # 1️⃣ Semantic-lite (의미 구역 보정)
        sections = apply_semantic_lite(sections)

        # 2️⃣ 불필요 섹션 제거
        sections = filter_irrelevant_sections(sections)

        # 3️⃣ Canonical Map 생성
        return self._build_canonical_map(sections)

    def _build_canonical_map(self, sections) -> dict[str, list[str]]:
        """
        semantic_zone 기준 key:value canonical 구조 생성
        """

        result = defaultdict(list)

        for sec in sections:
            zone = sec.semantic_zone

            # 일반 문장
            for line in sec.lines:
                result[zone].append(line)

            # 리스트 항목
            for lst in sec.lists:
                for item in lst:
                    result[zone].append(item)

        return dict(result)

    def to_jobsummary_canonical_map(self, canonical_map: dict[str, list[str]]) -> dict[str, list[str]]:
        """
        JdIntakePolicy 입력 계약에 맞게 canonical_map을 축약한다.

        target keys:
        - responsibilities
        - requirements
        - preferred
        - process
        - summary
        - tech_stack
        - recruitment_process
        - etc
        """
        if not canonical_map:
            return {}

        def _clean(lines: list[str]) -> list[str]:
            out: list[str] = []
            seen: set[str] = set()
            for line in lines:
                if not isinstance(line, str):
                    continue
                s = line.strip()
                if not s:
                    continue
                k = s.lower()
                if k in seen:
                    continue
                seen.add(k)
                out.append(s)
            return out

        result: dict[str, list[str]] = {}
        used_lines: set[str] = set()

        def consume(target_key: str, source_keys: tuple[str, ...], limit: int | None = None) -> None:
            merged: list[str] = []
            for key in source_keys:
                lines = canonical_map.get(key)
                if isinstance(lines, list):
                    merged.extend(lines)

            if not merged:
                return

            cleaned = []
            for line in _clean(merged):
                lk = line.lower()
                if lk in used_lines:
                    continue
                used_lines.add(lk)
                cleaned.append(line)
                if limit is not None and len(cleaned) >= limit:
                    break

            if cleaned:
                result[target_key] = cleaned

        consume("responsibilities", ("responsibilities",))
        consume("requirements", ("requirements",))
        consume("preferred", ("preferred",))
        consume("process", ("process", "application_guide"), limit=20)
        consume("summary", ("summary", "company", "intro"), limit=12)
        consume("tech_stack", ("tech_stack", "skills"), limit=20)
        consume("recruitment_process", ("recruitment_process",), limit=20)

        # Structural/header 분리가 약할 때 required/preferred를 복구한다.
        self._recover_required_sections(canonical_map, result, used_lines)

        known_source_keys = {
            "responsibilities",
            "requirements",
            "preferred",
            "process",
            "summary",
            "company",
            "intro",
            "tech_stack",
            "skills",
            "experience",
            "recruitment_process",
            "application_guide",
            "etc",
        }

        etc_lines: list[str] = []
        etc_seed = canonical_map.get("etc")
        if isinstance(etc_seed, list):
            etc_lines.extend(etc_seed)

        for key, lines in canonical_map.items():
            if key in known_source_keys:
                continue
            if isinstance(lines, list):
                etc_lines.extend(lines)

        # 위에서 분리하지 않은 나머지는 etc 보조 문맥으로 유지한다.

        if etc_lines:
            etc_cleaned = []
            for line in _clean(etc_lines):
                lk = line.lower()
                if lk in used_lines:
                    continue
                used_lines.add(lk)
                etc_cleaned.append(line)
                if len(etc_cleaned) >= 20:
                    break
            if etc_cleaned:
                result["etc"] = etc_cleaned

        return result

    def _recover_required_sections(
        self,
        canonical_map: dict[str, list[str]],
        result: dict[str, list[str]],
        used_lines: set[str],
    ) -> None:
        need_requirements = not result.get("requirements")
        need_preferred = not result.get("preferred")
        if not (need_requirements or need_preferred):
            return

        zone_order = (
            "responsibilities",
            "requirements",
            "preferred",
            "summary",
            "intro",
            "company",
            "skills",
            "tech_stack",
            "experience",
            "process",
            "application_guide",
            "others",
            "etc",
        )

        all_lines: list[str] = []
        for key in zone_order:
            lines = canonical_map.get(key)
            if isinstance(lines, list):
                all_lines.extend([line for line in lines if isinstance(line, str)])
        for key, lines in canonical_map.items():
            if key in zone_order or not isinstance(lines, list):
                continue
            all_lines.extend([line for line in lines if isinstance(line, str)])

        marker_zone = None
        reparsed = {"requirements": [], "preferred": []}

        for raw in all_lines:
            line = (raw or "").strip()
            if not line:
                continue

            marker = self._detect_header_marker(line)
            if marker is not None:
                marker_zone = marker
                continue

            if marker_zone in reparsed:
                reparsed[marker_zone].append(line)

        if need_requirements and reparsed["requirements"]:
            result["requirements"] = self._dedupe_for_result(
                reparsed["requirements"],
                used_lines,
                limit=20,
            )

        if need_preferred and reparsed["preferred"]:
            result["preferred"] = self._dedupe_for_result(
                reparsed["preferred"],
                used_lines,
                limit=20,
            )

    def _detect_header_marker(self, header_line: str) -> str | None:
        h = self._normalize_compact(header_line).strip("[]()<>")
        if not h:
            return None

        if len(h) > 30:
            return None

        requirements_markers = (
            "자격요건",
            "필수요건",
            "지원자격",
            "요구사항",
            "requirements",
            "qualification",
            "qualifications",
        )
        preferred_markers = (
            "우대사항",
            "우대조건",
            "가산점",
            "preferred",
            "nicetohave",
            "pluses",
            "plus",
        )
        stop_markers = (
            "주요업무",
            "담당업무",
            "업무내용",
            "기술스택",
            "채용절차",
            "전형절차",
            "복지",
            "혜택",
            "근무조건",
            "근무지",
            "고용형태",
            "process",
            "benefits",
            "location",
        )

        if any(m == h for m in requirements_markers):
            return "requirements"
        if any(m == h for m in preferred_markers):
            return "preferred"
        if any(m == h for m in stop_markers):
            return "stop"
        return None

    @staticmethod
    def _normalize_compact(text: str) -> str:
        s = re.sub(r"\s+", "", (text or "").lower())
        return s

    @staticmethod
    def _dedupe_for_result(lines: list[str], used_lines: set[str], limit: int) -> list[str]:
        out: list[str] = []
        seen: set[str] = set()
        for line in lines:
            s = (line or "").strip()
            if not s:
                continue
            key = s.lower()
            if key in seen or key in used_lines:
                continue
            seen.add(key)
            used_lines.add(key)
            out.append(s)
            if len(out) >= limit:
                break
        return out
