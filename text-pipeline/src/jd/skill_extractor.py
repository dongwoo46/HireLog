from typing import Dict, List

# 토큰을 표준 형태(canonical form)로 변환하는 함수
# 예: "JavaScript" → "javascript", "Spring-Boot" → "spring boot"
from common.token.canonical import to_canonical

# 기술 스택 사전과 alias 사전을 로드하는 함수
# - load_skill_vocab : { category: set(skills) }
# - load_skill_alias : { alias: canonical }
from common.vocab.loader import load_skill_vocab, load_skill_alias


def _generate_ngrams(tokens: List[str], max_n: int = 3) -> List[str]:
    """
    주어진 토큰 리스트로부터 1~max_n gram을 생성한다.

    목적:
    - 단일 토큰 매칭뿐 아니라
      "spring boot", "machine learning" 같은 복합 기술어를
      잡아내기 위함

    예:
    tokens = ["spring", "boot", "kafka"]
    결과 = [
        "spring", "boot", "kafka",          # 1-gram
        "spring boot", "boot kafka",        # 2-gram
        "spring boot kafka"                 # 3-gram
    ]
    """

    ngrams = []

    # n = gram의 길이 (1~max_n)
    for n in range(1, max_n + 1):

        # 슬라이딩 윈도우 방식으로 n-gram 생성
        for i in range(len(tokens) - n + 1):

            # tokens[i : i+n]을 공백으로 합쳐 하나의 phrase로 생성
            ngrams.append(" ".join(tokens[i:i + n]))

    return ngrams


def extract_skills_from_lines(
        lines: List[str],
) -> Dict[str, List[str]]:
    """
    JD 문장 리스트에서 기술 스택을 추출한다. (운영용 로직)

    전체 처리 흐름:
    1️⃣ 문장을 canonical token 단위로 정규화
    2️⃣ 토큰 기반 n-gram 생성
    3️⃣ alias → canonical 치환
    4️⃣ 카테고리별 vocab과 정확 매칭

    반환 형태:
    {
        "language": ["java", "kotlin"],
        "framework": ["spring", "spring boot"],
        "infra": ["kafka", "redis"]
    }
    """

    # 기술 스택 vocab 로드
    # 예: {
    #   "language": {"java", "kotlin", "python"},
    #   "framework": {"spring", "spring boot"},
    # }
    skill_vocab = load_skill_vocab()

    # alias → canonical 매핑 로드
    # 예: {
    #   "js": "javascript",
    #   "springboot": "spring boot"
    # }
    alias_map = load_skill_alias()

    # 결과 저장용 딕셔너리
    # - category별로 set을 사용해 중복 제거
    result: Dict[str, set] = {
        category: set()
        for category in skill_vocab
    }

    # JD의 각 문장(line)을 순회
    for line in lines:

        # 1️⃣ 문장을 공백 기준으로 분리한 뒤
        #    각 토큰을 canonical 형태로 변환
        #
        # 예:
        # "Spring-Boot with Java"
        # → ["spring", "boot", "with", "java"]
        tokens = [
            to_canonical(t)
            for t in line.split()
        ]

        # 2️⃣ 단일 토큰 + 복합 토큰 매칭을 위해 n-gram 생성
        candidates = _generate_ngrams(tokens)

        # 생성된 모든 후보 phrase를 순회
        for cand in candidates:

            # 3️⃣ alias가 존재하면 canonical 값으로 치환
            #
            # 예:
            # cand = "js" → "javascript"
            # cand = "spring boot" → 그대로 유지
            normalized = alias_map.get(cand, cand)

            # 4️⃣ 카테고리별 vocab과 정확 매칭
            for category, vocab in skill_vocab.items():

                # vocab에 정확히 포함된 경우만 기술 스택으로 인정
                if normalized in vocab:
                    result[category].add(normalized)

    # 최종 반환
    # - set → 정렬된 list로 변환
    # - 비어 있는 카테고리는 제거
    return {
        category: sorted(values)
        for category, values in result.items()
        if values
    }
