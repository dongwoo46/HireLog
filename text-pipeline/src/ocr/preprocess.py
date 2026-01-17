from pathlib import Path
import cv2


UPSCALE_FACTOR = 2.0


def preprocess_image(image_path: str):
    """
    PaddleOCR 기반 JD OCR을 위한 이미지 전처리 함수.

    이 함수의 역할:
    - PaddleOCR가 요구하는 3채널 이미지 형식을 유지한다.
    - JD의 작은 폰트 인식을 위해 해상도만 보조적으로 보정한다.

    설계 원칙:
    - PaddleOCR 내부 전처리를 신뢰한다.
    - 문자를 훼손할 수 있는 전처리는 하지 않는다.
    """

    image_path = Path(image_path)
    if not image_path.exists():
        raise FileNotFoundError(f"OCR image not found: {image_path}")

    # PaddleOCR는 3채널 이미지를 전제로 한다.
    image = cv2.imread(str(image_path), cv2.IMREAD_COLOR)
    if image is None:
        raise ValueError(f"Failed to load image: {image_path}")

    # JD의 작은 폰트 보조용 업스케일
    image = cv2.resize(
        image,
        None,
        fx=UPSCALE_FACTOR,
        fy=UPSCALE_FACTOR,
        interpolation=cv2.INTER_CUBIC
    )

    return image
