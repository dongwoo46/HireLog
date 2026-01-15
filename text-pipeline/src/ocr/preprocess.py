from pathlib import Path
import cv2

# OCR 전처리 관련 상수
UPSCALE_FACTOR = 2.0
ADAPTIVE_BLOCK_SIZE = 31
ADAPTIVE_C = 2


def preprocess_image(image_path: str):
    """
    JD(Job Description) 이미지 OCR을 위한 전처리 파이프라인.

    목적:
    - 작은 폰트, 영문/특수문자 위주의 JD 텍스트를
      최대한 손실 없이 OCR 엔진에 전달한다.

    설계 원칙:
    - '깨끗하게 보이게'가 아니라 '문자 형태 보존'에 초점
    - 기술 스택(gRPC, nginx, kafka 등) 손실 최소화
    """

    # 1️⃣ 입력 이미지 존재 검증
    # OCR 파이프라인의 안정성을 위해 조기 실패
    image_path = Path(image_path)
    if not image_path.exists():
        raise FileNotFoundError(f"OCR image not found: {image_path}")

    # 2️⃣ Grayscale 변환
    # 컬러 정보는 OCR 정확도에 거의 기여하지 않음
    image = cv2.imread(str(image_path), cv2.IMREAD_GRAYSCALE)

    # 3️⃣ 이미지 확대
    # JD는 작은 폰트가 많아 해상도 증가가 필수
    image = cv2.resize(
        image,
        None,
        fx=UPSCALE_FACTOR,
        fy=UPSCALE_FACTOR,
        interpolation=cv2.INTER_CUBIC
    )

    # 4️⃣ 대비 정규화
    # 스크린샷/캡쳐 이미지에서 글자 대비를 안정화
    image = cv2.normalize(
        image,
        None,
        alpha=0,
        beta=255,
        norm_type=cv2.NORM_MINMAX
    )

    # 5️⃣ Adaptive Threshold
    # OTSU 대신 국소 임계값을 사용해 얇은 글자 보존
    image = cv2.adaptiveThreshold(
        image,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        ADAPTIVE_BLOCK_SIZE,
        ADAPTIVE_C
    )

    return image
