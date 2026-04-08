# src/ocr/grpc/ocr_client.py

import json
import logging

import grpc

from ocr.grpc import ocr_service_pb2
from ocr.grpc import ocr_service_pb2_grpc

logger = logging.getLogger(__name__)

_DEFAULT_HOST = "localhost"
_DEFAULT_PORT = 50051
_DEFAULT_TIMEOUT_SEC = 60


class OcrGrpcClient:
    """
    OCR gRPC 클라이언트

    TEXT+URL 프로세스에서 사용.
    PaddleOCR을 직접 import/초기화하지 않고 OCR 프로세스에 위임함으로써
    프로세스 간 PaddleOCR 충돌을 방지한다.

    비책임:
    - OCR 실행 ❌
    - 섹션 파싱 ❌
    """

    def __init__(self, host: str = _DEFAULT_HOST, port: int = _DEFAULT_PORT):
        self._channel = grpc.insecure_channel(f"{host}:{port}")
        self._stub = ocr_service_pb2_grpc.OcrServiceStub(self._channel)
        logger.debug(
            "[OCR_GRPC_CLIENT] initialized | target=%s:%d", host, port
        )

    def run_ocr(self, images: list[str], timeout_sec: int = _DEFAULT_TIMEOUT_SEC) -> dict:
        """
        OCR 프로세스에 OCR 실행을 위임하고 결과를 반환.

        Returns:
            process_ocr_input()과 동일한 구조:
            {rawText, lines, confidence, status, errors}

        Raises:
            RuntimeError: gRPC 호출 실패 시
        """
        request = ocr_service_pb2.OcrRequest(images=images)

        logger.debug(
            "[OCR_GRPC_CLIENT] RunOcr called | image_count=%d", len(images)
        )

        try:
            response = self._stub.RunOcr(request, timeout=timeout_sec)
        except grpc.RpcError as e:
            raise RuntimeError(
                f"OCR gRPC call failed | code={e.code()} | detail={e.details()}"
            ) from e

        lines = json.loads(response.lines_json) if response.lines_json else []
        errors = []
        for raw in response.errors:
            try:
                errors.append(json.loads(raw))
            except Exception:
                errors.append({"error": raw})

        logger.debug(
            "[OCR_GRPC_CLIENT] RunOcr response | status=%s | line_count=%d",
            response.status,
            len(lines),
        )

        return {
            "status": response.status,
            "confidence": response.confidence,
            "rawText": response.raw_text,
            "lines": lines,
            "errors": errors,
        }

    def close(self) -> None:
        self._channel.close()
