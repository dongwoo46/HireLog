from jd.section_builder import build_sections
from jd.feature_builder import build_jd_features
from jd.header_rewriter import rewrite_broken_headers
from jd.bullet_normalizer import normalize_bullets

def is_job_description(jd_doc: dict) -> bool:
    """
    구조화된 JD 문서가 '채용 공고(JD)'인지 판별

    판단 기준:
    - 섹션 개수
    - 기술 스택 밀도
    """

    sections = jd_doc.get("sections", {})
    features = jd_doc.get("features", {})

    section_score = len(sections)
    skill_score = sum(
        len(v) for v in features.get("skills", {}).values()
    )

    # 강한 JD 시그널
    if section_score >= 2 and skill_score >= 3:
        return True

    # 약한 JD라도 기술 밀도 높으면 허용
    if skill_score >= 5:
        return True

    return False


def parse_jd_document(ocr_result: dict) -> dict:
    """
    JD 문서 파이프라인 단일 진입점

    역할:
    - OCR 결과(lines)를 입력으로 받아
    - JD 구조화 + feature 추출 수행
    - JD 여부를 판별하여 결과에 포함
    """

    jd_lines = ocr_result["lines"]

    # 🔥 OCR 깨진 섹션 헤더 복구
    jd_lines = rewrite_broken_headers(jd_lines)

    # 2️⃣ bullet 형태 정규화  👈 여기
    jd_lines = normalize_bullets(jd_lines)

    # 1️⃣ 섹션 구조화
    section_result = build_sections(jd_lines)

    # 2️⃣ feature 추출
    features = build_jd_features(section_result)

    jd_doc = {
        "rawText": ocr_result["rawText"],
        "sections": section_result["sections"],
        "canonical_text": section_result["canonical_text"],
        "features": features,
        "confidence": ocr_result["confidence"],
        "status": ocr_result["status"],
    }

    # 3️⃣ JD 여부 판별
    jd_doc["is_job_description"] = is_job_description(jd_doc)

    return jd_doc
