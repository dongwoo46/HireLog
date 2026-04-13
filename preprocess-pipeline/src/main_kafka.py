# src/main_kafka.py

"""
Kafka 기반 Preprocess Pipeline Entry Point

프로세스 배치 전략 (2-core 최적화):
- OCR Process : CPU-bound (PaddleOCR) → 독립 프로세스, 1 core 점유
- TEXT Process : CPU-bound (정규식/문자열) → 독립 프로세스, 1 core 점유
  └─ URL Thread : I/O-bound (HTTP fetch) → TEXT 프로세스 내 스레드 (GIL 해제)

실패 처리:
- 처리 실패 → fail 토픽 발행
- fail 토픽 발행 실패 → 로컬 파일 백업
- 무조건 commit (파이프라인 멈추지 않음)

Graceful Shutdown:
- SIGTERM/SIGINT → multiprocessing.Event 설정
- 각 Worker 프로세스/스레드는 Event를 감지하고 현재 메시지 완료 후 종료
"""

import logging
import multiprocessing
import signal
import threading
import time
import uuid

from infra.config.kafka_config import (
    load_kafka_config,
    load_kafka_worker_config,
    KafkaConnectionConfig,
    WorkerConfig,
)

# ==================================================
# Logging 설정 (메인 프로세스 전용, spawn re-import 방지)
# ==================================================
from utils.logger import setup_logging

if __name__ == "__main__" or multiprocessing.current_process().name == "MainProcess":
    setup_logging("DEBUG")

logger = logging.getLogger(__name__)


def _init_process_logging():
    """자식 프로세스 logging 초기화 (spawn 시 부모 설정 미상속)

    NOTE: 반드시 PaddleOCR 등 외부 라이브러리 import 이후에 호출해야 한다.
    ppocr은 import 시 자체 handler를 root logger에 등록하는데,
    setup_logging이 먼저 실행되면 ppocr import가 JSON handler를 덮어쓴다.
    """
    import sys
    try:
        if hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(line_buffering=True)
    except Exception:
        pass
    setup_logging("DEBUG")


def _create_worker(factory, worker_cls, topic, consumer_group, poll_timeout_sec,
                   result_topic, fail_topic, worker_config, client_id, shutdown_event):
    """Consumer/Producer/Worker 인스턴스 생성 (프로세스 내부 호출 전용)"""
    from infra.kafka.kafka_consumer import KafkaStreamConsumer
    from infra.kafka.kafka_producer import KafkaStreamProducer

    raw_consumer = factory.create_consumer(
        group_id=consumer_group,
        client_id=client_id,
    )
    consumer = KafkaStreamConsumer(
        consumer=raw_consumer,
        topic=topic,
        poll_timeout_sec=poll_timeout_sec,
    )

    raw_producer = factory.create_producer(
        client_id=f"{client_id}-producer",
    )
    producer = KafkaStreamProducer(raw_producer)

    worker = worker_cls(
        consumer=consumer,
        producer=producer,
        result_topic=result_topic,
        fail_topic=fail_topic,
        config=worker_config,
        shutdown_event=shutdown_event,
    )
    return worker, producer


def _run_ocr_process(
    shutdown_event: multiprocessing.Event,
    connection_config: KafkaConnectionConfig,
    topic: str,
    consumer_group: str,
    poll_timeout_sec: float,
    result_topic: str,
    fail_topic: str,
    worker_config: WorkerConfig,
    client_id: str,
    grpc_port: int = 50051,
) -> None:
    """
    OCR 전용 프로세스 (CPU-bound)

    PaddleOCR 모델이 CPU를 집중 사용하므로 독립 프로세스로 격리
    """
    # 수치 연산 라이브러리 스레드 제한 (PaddleOCR import 전에 설정 필수)
    # 미설정 시 코어 수만큼 스레드를 생성하여 다른 프로세스의 core를 침범
    import os
    os.environ["OMP_NUM_THREADS"] = "1"
    os.environ["OPENBLAS_NUM_THREADS"] = "1"
    os.environ["MKL_NUM_THREADS"] = "1"

    # 1차: startup 로그를 보이게 하기 위해 import 전에 logging 먼저 설정
    _init_process_logging()
    proc_logger = logging.getLogger(f"process.ocr.{client_id}")
    proc_logger.info("Loading OCR engine (PaddleOCR model init)", extra={"proc":"ocr"})

    from infra.kafka.kafka_client_factory import KafkaClientFactory
    from worker.ocr_kafka_worker import OcrKafkaWorker

    # 2차: ppocr이 import/init 시 root logger를 건드릴 수 있으므로 재적용
    setup_logging("DEBUG")
    logging.disable(logging.NOTSET)

    # PaddleOCR은 첫 .ocr() 호출 시 모델 weight를 lazy load하면서
    # logging.disable() 등을 내부에서 호출한다.
    # worker.run() 전에 빈 이미지로 워밍업을 강제하여 모델 로드를 완료시키고,
    # 그 이후 logging을 재적용하면 worker 실행 중 로그가 보존된다.
    proc_logger.info("Warming up PaddleOCR model", extra={"proc":"ocr"})
    try:
        import numpy as np
        from ocr.engine import _ocr as _paddle_ocr
        _dummy = np.zeros((64, 64, 3), dtype=np.uint8)
        _paddle_ocr.ocr(_dummy)
    except Exception:
        pass

    # 3차: 모델 로드 완료 후 logging 최종 재적용
    setup_logging("DEBUG")
    logging.disable(logging.NOTSET)
    proc_logger.info("OCR engine ready", extra={"proc":"ocr"})

    from ocr.grpc.ocr_server import OcrGrpcServer

    grpc_server = OcrGrpcServer(port=grpc_port)
    grpc_server.start()

    try:
        factory = KafkaClientFactory(connection_config)
        worker, producer = _create_worker(
            factory, OcrKafkaWorker, topic, consumer_group, poll_timeout_sec,
            result_topic, fail_topic, worker_config, client_id, shutdown_event,
        )

        worker.run()

    except KeyboardInterrupt:
        proc_logger.info("Process interrupted", extra={"proc":"ocr"})
    except Exception as e:
        proc_logger.error("Process crashed", exc_info=True, extra={"proc":"ocr", "error": str(e)})
    finally:
        grpc_server.stop()
        try:
            producer.close()
        except Exception:
            pass
        proc_logger.info("Process exiting", extra={"proc":"ocr"})


def _run_text_url_process(
    shutdown_event: multiprocessing.Event,
    connection_config: KafkaConnectionConfig,
    text_topic: str,
    url_topic: str,
    consumer_group: str,
    poll_timeout_sec: float,
    result_topic: str,
    fail_topic: str,
    worker_config: WorkerConfig,
    text_client_id: str,
    url_client_id: str,
) -> None:
    """
    TEXT + URL 혼합 프로세스

    - TEXT Worker: 메인 스레드 (CPU-bound)
    - URL Worker:  별도 스레드 (I/O-bound, HTTP 대기 중 GIL 해제)

    동일 프로세스 내에서 Producer를 공유하여 리소스 절약
    (confluent-kafka Producer는 thread-safe)
    """
    _init_process_logging()
    proc_logger = logging.getLogger(f"process.text_url")

    from infra.kafka.kafka_client_factory import KafkaClientFactory
    from worker.text_kafka_worker import TextKafkaWorker
    from worker.url_kafka_worker import UrlKafkaWorker

    try:
        factory = KafkaClientFactory(connection_config)

        # URL Worker (스레드로 실행, I/O-bound)
        url_worker, url_producer = _create_worker(
            factory, UrlKafkaWorker, url_topic, consumer_group, poll_timeout_sec,
            result_topic, fail_topic, worker_config, url_client_id, shutdown_event,
        )

        url_thread = threading.Thread(
            target=url_worker.run,
            name="url-worker-thread",
            daemon=True,
        )
        url_thread.start()
        proc_logger.info("URL worker thread started")

        # TEXT Worker (메인 스레드, CPU-bound)
        text_worker, text_producer = _create_worker(
            factory, TextKafkaWorker, text_topic, consumer_group, poll_timeout_sec,
            result_topic, fail_topic, worker_config, text_client_id, shutdown_event,
        )

        text_worker.run()

        # TEXT 종료 후 URL 스레드 대기
        url_thread.join(timeout=worker_config.shutdown_timeout_sec)

    except KeyboardInterrupt:
        proc_logger.info("Process interrupted", extra={"proc":"text_url"})
    except Exception as e:
        proc_logger.error(
            "Process crashed",
            exc_info=True,
            extra={"proc":"text_url", "error": str(e)},
        )
    finally:
        for p in [text_producer, url_producer]:
            try:
                p.close()
            except Exception:
                pass
        proc_logger.info("Process exiting", extra={"proc":"text_url"})


def _run_embedding_server() -> None:
    """
    임베딩 서버 프로세스

    FastAPI + uvicorn (I/O-bound)
    Spring에서 HTTP로 호출 → 벡터 반환
    """
    _init_process_logging()
    proc_logger = logging.getLogger("process.embedding")
    proc_logger.info("Starting embedding server")

    import os
    import uvicorn
    from embedding.model import load_model

    host = os.environ.get("EMBEDDING_SERVER_HOST", "0.0.0.0")
    port = int(os.environ.get("EMBEDDING_SERVER_PORT", "8000"))

    load_model()

    uvicorn.run(
        "embedding.main:app",
        host=host,
        port=port,
        log_config=None,
    )


def main():
    """메인 진입점"""
    logger.info("Preprocess Pipeline starting")

    # ==================================================
    # 설정 로드
    # ==================================================
    kafka_config = load_kafka_config()
    worker_config = load_kafka_worker_config()

    logger.info(
        "Config loaded",
        extra={
            "bootstrap_servers": kafka_config.bootstrap_servers,
            "text_topic": kafka_config.text_topic,
            "ocr_topic": kafka_config.ocr_topic,
            "url_topic": kafka_config.url_topic,
            "result_topic": kafka_config.result_topic,
            "fail_topic": kafka_config.fail_topic,
            "consumer_group": kafka_config.consumer_group,
        },
    )

    # ==================================================
    # Instance 고유 ID
    # ==================================================
    instance_id = uuid.uuid4().hex[:8]
    logger.info("Instance created", extra={"instance_id": instance_id})

    # ==================================================
    # OCR gRPC 포트 (URL Worker → OCR Worker IPC)
    # ==================================================
    import os
    ocr_grpc_port = int(os.environ.get("OCR_GRPC_PORT", "50051"))

    # ==================================================
    # Shutdown Event (프로세스 간 공유)
    # ==================================================
    shutdown_event = multiprocessing.Event()

    # ==================================================
    # 프로세스 배치 (2-core 최적화)
    #
    # Process 1: OCR (CPU-bound, 1 core)
    # Process 2: TEXT (CPU-bound, 메인 스레드)
    #            └─ URL (I/O-bound, 스레드)
    # Process 3: Embedding Server (I/O-bound, FastAPI)
    # ==================================================
    embedding_process = multiprocessing.Process(
        target=_run_embedding_server,
        name="embedding-process",
        daemon=False,
    )

    ocr_process = multiprocessing.Process(
        target=_run_ocr_process,
        args=(
            shutdown_event,
            kafka_config.connection,
            kafka_config.ocr_topic,
            kafka_config.consumer_group,
            kafka_config.poll_timeout_sec,
            kafka_config.result_topic,
            kafka_config.fail_topic,
            worker_config,
            f"ocr-consumer-{instance_id}",
            ocr_grpc_port,
        ),
        name="ocr-process",
        daemon=False,
    )

    text_url_process = multiprocessing.Process(
        target=_run_text_url_process,
        args=(
            shutdown_event,
            kafka_config.connection,
            kafka_config.text_topic,
            kafka_config.url_topic,
            kafka_config.consumer_group,
            kafka_config.poll_timeout_sec,
            kafka_config.result_topic,
            kafka_config.fail_topic,
            worker_config,
            f"text-consumer-{instance_id}",
            f"url-consumer-{instance_id}",
        ),
        name="text-url-process",
        daemon=False,
    )

    # ==================================================
    # Shutdown 핸들러 (메인 프로세스)
    # ==================================================
    def shutdown_handler(signum, frame):
        sig_name = signal.Signals(signum).name
        logger.info("Shutdown signal received", extra={"signal": sig_name})
        shutdown_event.set()

    signal.signal(signal.SIGTERM, shutdown_handler)
    signal.signal(signal.SIGINT, shutdown_handler)

    # ==================================================
    # 프로세스 시작
    # ==================================================
    processes = [ocr_process, text_url_process, embedding_process]

    for p in processes:
        p.start()
        logger.info("Worker process started", extra={"process_name": p.name, "child_pid": p.pid})

    logger.info("All worker processes running")

    # ==================================================
    # 메인 프로세스 대기
    # ==================================================
    try:
        while not shutdown_event.is_set():
            alive_count = sum(1 for p in processes if p.is_alive())
            if alive_count == 0:
                logger.warning("All worker processes have stopped unexpectedly")
                break
            time.sleep(1)

    except KeyboardInterrupt:
        logger.info("KeyboardInterrupt received")
        shutdown_event.set()

    # ==================================================
    # 프로세스 종료 대기
    # ==================================================
    logger.info("Waiting for worker processes to finish")

    for p in processes:
        p.join(timeout=worker_config.shutdown_timeout_sec)
        if p.is_alive():
            logger.warning(
                "Process did not finish in time, terminating",
                extra={"process_name": p.name, "child_pid": p.pid},
            )
            p.terminate()
            p.join(timeout=5)
        else:
            logger.info(
                "Process finished",
                extra={"process_name": p.name, "exit_code": p.exitcode},
            )

    logger.info("Preprocess Pipeline stopped")


if __name__ == "__main__":
    multiprocessing.set_start_method("spawn", force=True)
    main()
