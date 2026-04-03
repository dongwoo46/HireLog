-- uk_job_summary_snapshot_id를 partial unique index로 교체
-- is_active = true인 경우에만 유일성 강제
-- 비활성(soft-delete) Summary가 있는 snapshot에 신규 Summary 생성 가능하도록 수정

DROP INDEX uk_job_summary_snapshot_id;

CREATE UNIQUE INDEX uk_job_summary_snapshot_id
    ON job_summary (job_snapshot_id)
    WHERE is_active = true;
