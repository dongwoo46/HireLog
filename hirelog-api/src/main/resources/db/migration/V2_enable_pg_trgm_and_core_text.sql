CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_job_snapshot_core_text_trgm
    ON job_snapshot
    USING gin (core_text gin_trgm_ops);

