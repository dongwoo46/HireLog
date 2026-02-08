# src/utils/fail_backup.py

"""
실패 메시지 로컬 백업 (JSONL)

용도:
- Kafka fail 토픽 발행 실패 시 로컬 백업
- 마지막 보루 (메시지 유실 방지)
- 수동 또는 Java 배치로 처리

파일 형식:
- JSON Lines (.jsonl)
- 한 줄 = 하나의 실패 레코드
- grep, jq로 검색 가능

파일명: {날짜}.jsonl (예: 2026-02-04.jsonl)

포함 필드 (원본 메시지 포함 안 함):
- requestId
- source
- errorCode
- errorMessage
- occurredAt
- publishError
- workerHost
"""

import json
import logging
import os
import socket
import threading
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)

# 기본 백업 디렉토리
DEFAULT_BACKUP_DIR = Path(__file__).parent.parent.parent / "logs" / "kafka_failures"


def _get_hostname() -> str:
    """호스트명 조회 (실패 시 unknown)"""
    try:
        return socket.gethostname()
    except Exception:
        return "unknown"


class FailBackupWriter:
    """
    실패 메시지 JSONL 백업 Writer

    Thread-safe (멀티 Worker 환경 대응)
    파일명: {날짜}.jsonl
    """

    def __init__(self, backup_dir: Optional[Path] = None):
        """
        Args:
            backup_dir: 백업 디렉토리 (기본: logs/kafka_failures/)
        """
        self.backup_dir = Path(
            backup_dir
            or os.environ.get("FAIL_BACKUP_DIR")
            or DEFAULT_BACKUP_DIR
        )
        self._lock = threading.Lock()
        self._ensure_dir()

    def _ensure_dir(self) -> None:
        """백업 디렉토리 생성"""
        try:
            self.backup_dir.mkdir(parents=True, exist_ok=True)
        except Exception as e:
            logger.error("[FAIL_BACKUP] Cannot create backup dir: %s", e)

    def _get_file_path(self) -> Path:
        """현재 백업 파일 경로 (날짜별)"""
        date_str = datetime.now().strftime("%Y-%m-%d")
        return self.backup_dir / f"{date_str}.jsonl"

    def write(
        self,
        request_id: str,
        source: str,
        error_code: str,
        error_message: str,
        publish_error: str,
    ) -> bool:
        """
        실패 메시지 백업

        Args:
            request_id: 요청 ID
            source: 소스 타입 (OCR/TEXT/URL)
            error_code: 에러 코드
            error_message: 에러 메시지
            publish_error: fail 토픽 발행 실패 원인

        Returns:
            True: 백업 성공
            False: 백업 실패 (로그만 남김)
        """
        data = {
            "requestId": request_id,
            "source": source,
            "errorCode": error_code,
            "errorMessage": error_message,
            "occurredAt": datetime.now(timezone.utc).isoformat(),
            "publishError": publish_error,
            "workerHost": _get_hostname(),
        }

        try:
            line = json.dumps(data, ensure_ascii=False)
            file_path = self._get_file_path()

            with self._lock:
                with open(file_path, "a", encoding="utf-8") as f:
                    f.write(line + "\n")

            logger.warning(
                "[FAIL_BACKUP] Written to %s | requestId=%s errorCode=%s",
                file_path.name,
                request_id,
                error_code,
            )
            return True

        except Exception as e:
            # 백업조차 실패 → 에러 로그에라도 남김
            logger.error(
                "[FAIL_BACKUP_CRITICAL] Write failed | "
                "requestId=%s source=%s errorCode=%s errorMessage=%s publishError=%s exception=%s",
                request_id,
                source,
                error_code,
                error_message,
                publish_error,
                str(e),
            )
            return False


# 싱글톤 인스턴스
_default_writer: Optional[FailBackupWriter] = None


def get_fail_backup_writer() -> FailBackupWriter:
    """기본 백업 Writer 인스턴스 반환"""
    global _default_writer
    if _default_writer is None:
        _default_writer = FailBackupWriter()
    return _default_writer
