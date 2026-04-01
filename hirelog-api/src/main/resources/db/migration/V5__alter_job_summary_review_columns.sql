-- job_summary_review: split legacy comment columns into pros/cons/tip

ALTER TABLE job_summary_review
    RENAME COLUMN experience_comment TO pros_comment;

ALTER TABLE job_summary_review
    ADD COLUMN cons_comment TEXT;

UPDATE job_summary_review
SET cons_comment = 'Legacy data (cons not provided)'
WHERE cons_comment IS NULL;

ALTER TABLE job_summary_review
    ALTER COLUMN cons_comment SET NOT NULL;

ALTER TABLE job_summary_review
    RENAME COLUMN interview_tip TO tip;

CREATE TABLE review_like
(
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL,
    review_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_review_like_member_review UNIQUE (member_id, review_id)
);

CREATE INDEX idx_review_like_review ON review_like (review_id);
CREATE INDEX idx_review_like_member ON review_like (member_id);

-- report
CREATE TABLE report
(
    id                      BIGSERIAL PRIMARY KEY,
    reporter_id             BIGINT       NOT NULL REFERENCES member (id),
    job_summary_id          BIGINT                REFERENCES job_summary (id),
    job_summary_review_id   BIGINT                REFERENCES job_summary_review (id),
    reported_member_id      BIGINT                REFERENCES member (id),
    reason                  VARCHAR(30)  NOT NULL,
    detail                  TEXT,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_at             TIMESTAMP,
    reviewed_by             BIGINT,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    CONSTRAINT chk_report_single_target CHECK (
        (CASE WHEN job_summary_id IS NOT NULL THEN 1 ELSE 0 END +
         CASE WHEN job_summary_review_id IS NOT NULL THEN 1 ELSE 0 END +
         CASE WHEN reported_member_id IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
);

CREATE INDEX idx_report_status_created     ON report (status, created_at);
CREATE INDEX idx_report_reporter           ON report (reporter_id);
CREATE INDEX idx_report_job_summary        ON report (job_summary_id);
CREATE INDEX idx_report_review             ON report (job_summary_review_id);
CREATE INDEX idx_report_member             ON report (reported_member_id);

-- 중복 신고 방지 partial unique index (nullable 컬럼이므로 일반 unique constraint 불가)
CREATE UNIQUE INDEX uq_report_reporter_job_summary
    ON report (reporter_id, job_summary_id)
    WHERE job_summary_id IS NOT NULL;

CREATE UNIQUE INDEX uq_report_reporter_review
    ON report (reporter_id, job_summary_review_id)
    WHERE job_summary_review_id IS NOT NULL;

CREATE UNIQUE INDEX uq_report_reporter_member
    ON report (reporter_id, reported_member_id)
    WHERE reported_member_id IS NOT NULL;
