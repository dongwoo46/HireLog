# src/ocr/grpc/ocr_server.py

import json
import logging
import threading
from concurrent import futures

import grpc

from ocr.grpc import ocr_service_pb2
from ocr.grpc import ocr_service_pb2_grpc

logger = logging.getLogger(__name__)

_DEFAULT_PORT = 50051
_GRPC_WORKERS = 1  # PaddleOCR은 thread-safe 보장 없음 → 직렬 처리


class _NumpyEncoder(json.JSONEncoder):
    """numpy ndarray → list 변환 (box 필드 직렬화용)"""
    def default(self, obj):
        if hasattr(obj, "tolist"):
            return obj.tolist()
        return super().default(obj)


class _OcrServicer(ocr_service_pb2_grpc.OcrServiceServicer):
    """
    gRPC OcrService 구현체

    - URL Worker 로부터 이미지 경로를 받아 OCR 실행
    - process_ocr_input() 결과를 proto 응답으로 반환
    - PaddleOCR thread-safety를 위해 lock으로 직렬화
    """

    def __init__(self):
        self._lock = threading.Lock()

    def RunOcr(self, request, context):
        from ocr.pipeline import process_ocr_input

        images = list(request.images)

        logger.debug(
            "[OCR_GRPC_SERVER] RunOcr called | image_count=%d", len(images)
        )

        with self._lock:
            try:
                result = process_ocr_input(images)
            except Exception as e:
                logger.error(
                    "[OCR_GRPC_SERVER] process_ocr_input failed | error=%s", e, exc_info=True
                )
                context.set_code(grpc.StatusCode.INTERNAL)
                context.set_details(str(e))
                return ocr_service_pb2.OcrResponse()

        lines_json = json.dumps(
            result.get("lines", []),
            ensure_ascii=False,
            cls=_NumpyEncoder,
        )
        errors = [
            json.dumps(err, ensure_ascii=False)
            for err in result.get("errors", [])
            if err
        ]

        logger.debug(
            "[OCR_GRPC_SERVER] RunOcr completed | status=%s | line_count=%d",
            result.get("status"),
            len(result.get("lines", [])),
        )

        return ocr_service_pb2.OcrResponse(
            status=result.get("status", "FAIL"),
            confidence=float(result.get("confidence", 0.0)),
            raw_text=result.get("rawText", ""),
            lines_json=lines_json,
            errors=errors,
        )


class OcrGrpcServer:
    """
    OCR gRPC 서버 라이프사이클 관리

    OCR 프로세스에서 백그라운드 스레드로 기동.
    OcrKafkaWorker 메인 루프와 독립적으로 실행됨.
    """

    def __init__(self, port: int = _DEFAULT_PORT):
        self._port = port
        self._server: grpc.Server | None = None

    def start(self) -> None:
        self._server = grpc.server(
            futures.ThreadPoolExecutor(max_workers=_GRPC_WORKERS)
        )
        ocr_service_pb2_grpc.add_OcrServiceServicer_to_server(
            _OcrServicer(), self._server
        )
        self._server.add_insecure_port(f"[::]:{self._port}")
        self._server.start()
        logger.info("[OCR_GRPC_SERVER] started | port=%d", self._port)

    def stop(self, grace_sec: int = 5) -> None:
        if self._server:
            self._server.stop(grace_sec)
            logger.info("[OCR_GRPC_SERVER] stopped")
