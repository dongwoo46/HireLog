from inputs.jd_preprocess_input import JdPreprocessInput

from preprocess.core_preprocess.core_preprocessor import CorePreprocessor
from preprocess.structural_preprocess.structural_preprocessor import StructuralPreprocessor
from preprocess.semantic.semantic_preprocessor import apply_semantic_lite
from preprocess.semantic.section_filter import filter_irrelevant_sections
from preprocess.metadata_preprocess.metadata_preprocessor import MetadataPreprocessor
from collections import defaultdict


class TextPreprocessPipeline:
    """
    TEXT 기반 JD 전처리 파이프라인

    책임:
    - RAW JD 텍스트 입력
    - Core → Structural → Semantic-lite → Filter 실행
    - downstream에서 사용 가능한 canonical 결과 생성
    """

    def __init__(self):
        # 각 단계 Preprocessor는 재사용 가능하도록 멤버로 보관
        self.core = CorePreprocessor()
        self.structural = StructuralPreprocessor()
        self.metadata = MetadataPreprocessor()  # ✅ 추가


    def process(self, input: JdPreprocessInput) -> dict:
        """
        JD 전처리 메인 진입점
        """

        raw_text = input.text

        # 1️⃣ Core
        core_lines = self.core.process(raw_text)

        # 1.5️⃣ Metadata (문서 전역 메타)
        document_meta = self.metadata.process(core_lines)

        # 2️⃣ Structural
        sections = self.structural.process(core_lines)

        # 3️⃣ Semantic-lite
        sections = apply_semantic_lite(sections)

        # 4️⃣ Filter
        sections = filter_irrelevant_sections(sections)

        # 5️⃣ Canonical
        # canonical_text = self._build_canonical_text(sections)
        canonical_map = self._build_canonical_map(sections)


        return {
            "canonical_map": canonical_map,
            "document_meta": document_meta,  # ✅ 같이 반환
        }

    def _build_canonical_text(self, sections) -> str:
        """
        최종 canonical JD 텍스트 생성

        정책:
        - semantic_zone 유지
        - header + line + list 순서 보장
        - LLM 입력 친화적 포맷
        """

        blocks = []

        for sec in sections:
            header = sec.header
            zone = sec.semantic_zone

            # 섹션 헤더
            if header:
                blocks.append(f"[{zone}] {header}")
            else:
                blocks.append(f"[{zone}]")

            # 일반 라인
            for line in sec.lines:
                blocks.append(line)

            # 리스트 항목
            for lst in sec.lists:
                for item in lst:
                    blocks.append(f"- {item}")

            blocks.append("")  # 섹션 구분용 개행

        return "\n".join(blocks).strip()

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