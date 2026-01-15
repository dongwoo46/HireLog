from pathlib import Path

def load_noise_keywords(path: str | Path | None = None):
    """
    노이즈 키워드 파일을 로드한다.
    - 주석(#)과 빈 줄은 무시
    - 모두 소문자로 변환
    - path가 없으면 이 파일 기준 keywords.txt 사용
    """

    if path is None:
        # loader.py 기준 디렉토리
        base_dir = Path(__file__).resolve().parent
        path = base_dir / "keywords.txt"
    else:
        path = Path(path)

    keywords = set()

    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()

            if not line or line.startswith("#"):
                continue

            keywords.add(line.lower())

    return keywords
