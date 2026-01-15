"""
OCR word-level 결과를 line 단위 구조로 변환하는 모듈

용도:
- pytesseract raw 결과(word 단위)를 사람이 읽을 수 있는 line 단위로 재구성
- 이후 normalize / noise filter / JD 후처리 단계의 기준 입력 데이터 생성
"""

def build_lines(raw):
    """
    pytesseract image_to_data 결과를 line 단위 구조로 변환한다.

    입력:
    - raw: pytesseract image_to_data 결과(dict)
      (text, conf, block_num, par_num, line_num, left, top, width, height 포함)

    출력:
    - [
        {
          text: str,                     # 한 줄의 텍스트
          confidence_avg: float,         # 줄 평균 confidence
          confidence_min: int,           # 줄 내 최소 confidence
          low_conf_ratio: float,         # low confidence 토큰 비율
          bbox: (x, y, w, h),             # 줄 bounding box
          token_count: int,              # 줄을 구성하는 토큰 수
          key: (block_num, par_num, line_num)  # pytesseract 구조 키
        }
      ]
    """

    # (block_num, par_num, line_num) 기준으로 word들을 묶기 위한 딕셔너리
    grouped = {}

    # pytesseract는 word 단위 결과를 배열로 제공하므로 전체 인덱스 순회
    for i in range(len(raw["text"])):

        # 현재 word 텍스트 및 confidence 추출
        text = raw["text"][i].strip()
        conf = int(raw["conf"][i])

        # 인식 실패(word 없음) 또는 의미 없는 confidence 제거
        if not text or conf <= 0:
            continue

        # pytesseract 구조상 line_num은 block/par 단위로 재사용되므로
        # 반드시 block_num + par_num + line_num을 함께 사용해야 안전
        block = raw["block_num"][i]
        par = raw["par_num"][i]
        line = raw["line_num"][i]

        key = (block, par, line)

        # 해당 line key가 처음 등장한 경우 초기 구조 생성
        if key not in grouped:
            grouped[key] = {
                "tokens": [],   # line을 구성하는 word 텍스트들
                "confs": [],    # word별 confidence
                "left": [],     # word bbox left 좌표
                "top": [],      # word bbox top 좌표
                "width": [],    # word bbox width
                "height": []    # word bbox height
            }

        # word 단위 정보 누적
        grouped[key]["tokens"].append(text)
        grouped[key]["confs"].append(conf)
        grouped[key]["left"].append(raw["left"][i])
        grouped[key]["top"].append(raw["top"][i])
        grouped[key]["width"].append(raw["width"][i])
        grouped[key]["height"].append(raw["height"][i])

    lines = []

    # (block → par → line) 순서로 정렬하여
    # 시각적 위→아래 순서가 유지되도록 보장
    for key in sorted(grouped.keys()):
        v = grouped[key]

        # 줄 confidence 통계 계산
        avg_conf = sum(v["confs"]) / len(v["confs"])     # 평균 confidence
        min_conf = min(v["confs"])                        # 최소 confidence
        low_ratio = (
                sum(1 for c in v["confs"] if c < 60) / len(v["confs"])
        )  # low confidence 토큰 비율

        # 줄 단위 bounding box 계산
        bbox = _calculate_bbox(
            v["left"], v["top"], v["width"], v["height"]
        )

        # 최종 line 구조 생성
        lines.append({
            "text": " ".join(v["tokens"]),    # word들을 공백으로 결합한 line 텍스트
            "confidence_avg": avg_conf,
            "confidence_min": min_conf,
            "low_conf_ratio": low_ratio,
            "bbox": bbox,
            "token_count": len(v["tokens"]),
            "key": key
        })

    return lines


def _calculate_bbox(lefts, tops, widths, heights):
    """
    여러 word bounding box를 포함하는
    line 단위 bounding box를 계산한다.

    입력:
    - lefts: word들의 left 좌표 리스트
    - tops: word들의 top 좌표 리스트
    - widths: word들의 width 리스트
    - heights: word들의 height 리스트

    출력:
    - (x, y, w, h): line 전체를 감싸는 bbox
    """

    # 가장 왼쪽 / 가장 위 좌표
    x = min(lefts)
    y = min(tops)

    # 가장 오른쪽 끝 좌표 - x = 전체 width
    w = max(l + w for l, w in zip(lefts, widths)) - x

    # 줄 높이는 가장 큰 word height 기준
    h = max(heights)

    return (x, y, w, h)
