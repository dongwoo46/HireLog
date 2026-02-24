-- ============================================================
-- member
-- ============================================================
CREATE UNIQUE INDEX idx_member_email     ON member (email);
CREATE UNIQUE INDEX ux_member_username   ON member (username);

-- ============================================================
-- company
-- ============================================================
CREATE UNIQUE INDEX idx_company_normalized_name ON company (normalized_name);

-- ============================================================
-- brand
-- ============================================================
CREATE UNIQUE INDEX idx_brand_normalized_name              ON brand (normalized_name);
CREATE        INDEX idx_brand_company_id                   ON brand (company_id);
CREATE        INDEX idx_brand_verification_status_created_at ON brand (verification_status, created_at);

-- ============================================================
-- position_category
-- ============================================================
CREATE UNIQUE INDEX idx_position_category_normalized_name ON position_category (normalized_name);
CREATE        INDEX idx_position_category_status          ON position_category (status);

-- ============================================================
-- position
-- ============================================================
CREATE UNIQUE INDEX idx_position_normalized_name ON position (normalized_name);
CREATE        INDEX idx_position_status           ON position (status);

-- ============================================================
-- company_candidate
-- ============================================================
CREATE INDEX idx_company_candidate_brand  ON company_candidate (brand_id);
CREATE INDEX idx_company_candidate_status ON company_candidate (status);

-- ============================================================
-- job_snapshot
-- ============================================================
CREATE        INDEX idx_job_snapshot_brand_id      ON job_snapshot (brand_id);
CREATE        INDEX idx_job_snapshot_position_id   ON job_snapshot (position_id);
CREATE UNIQUE INDEX uk_job_snapshot_canonical_hash ON job_snapshot (canonical_hash);
CREATE        INDEX idx_job_snapshot_sim_hash       ON job_snapshot (sim_hash);
CREATE        INDEX idx_job_snapshot_core_text_trgm ON job_snapshot USING gin (core_text gin_trgm_ops);

-- ============================================================
-- job_summary
-- ============================================================
CREATE UNIQUE INDEX uk_job_summary_snapshot_id ON job_summary (job_snapshot_id);
CREATE        INDEX idx_job_summary_source_url  ON job_summary (source_url);
CREATE        INDEX idx_job_summary_is_active   ON job_summary (is_active);

-- ============================================================
-- job_summary_request
-- ============================================================
CREATE INDEX idx_job_summary_request_request_id ON job_summary_request (request_id);
CREATE INDEX idx_job_summary_request_member_id  ON job_summary_request (member_id);

-- ============================================================
-- job_summary_review
-- ============================================================
CREATE INDEX idx_review_job ON job_summary_review (job_summary_id);

-- ============================================================
-- brand_position
-- ============================================================
CREATE INDEX idx_brand_position_brand_id    ON brand_position (brand_id);
CREATE INDEX idx_brand_position_position_id ON brand_position (position_id);
CREATE INDEX idx_brand_position_status      ON brand_position (status);

-- ============================================================
-- member_job_summary
-- ============================================================
CREATE        INDEX idx_member_job_summary_job_summary ON member_job_summary (job_summary_id);
CREATE UNIQUE INDEX ux_member_job_summary_member_job   ON member_job_summary (member_id, job_summary_id);

-- ============================================================
-- member_company
-- ============================================================
CREATE        INDEX idx_member_company_member        ON member_company (member_id);
CREATE        INDEX idx_member_company_company        ON member_company (company_id);
CREATE UNIQUE INDEX ux_member_company_member_company  ON member_company (member_id, company_id);

-- ============================================================
-- member_brand
-- ============================================================
CREATE        INDEX idx_member_brand_member       ON member_brand (member_id);
CREATE        INDEX idx_member_brand_brand        ON member_brand (brand_id);
CREATE UNIQUE INDEX ux_member_brand_member_brand  ON member_brand (member_id, brand_id);

-- ============================================================
-- notification
-- ============================================================
CREATE INDEX idx_notification_member_read    ON notification (member_id, is_read);
CREATE INDEX idx_notification_member_created ON notification (member_id, created_at);

-- ============================================================
-- user_request
-- ============================================================
CREATE INDEX idx_user_request_member ON user_request (member_id);
CREATE INDEX idx_user_request_status ON user_request (status);

-- ============================================================
-- user_request_comment
-- ============================================================
CREATE INDEX idx_user_request_comment_request ON user_request_comment (user_request_id);
CREATE INDEX idx_user_request_comment_writer  ON user_request_comment (writer_type);

-- ============================================================
-- outbox_event
-- ============================================================
CREATE INDEX idx_outbox_aggregate_type ON outbox_event (aggregate_type);
CREATE INDEX idx_outbox_created_at     ON outbox_event (created_at);

-- ============================================================
-- failed_kafka_event
-- ============================================================
CREATE INDEX idx_failed_kafka_event_topic      ON failed_kafka_event (topic);
CREATE INDEX idx_failed_kafka_event_status     ON failed_kafka_event (status);
CREATE INDEX idx_failed_kafka_event_created_at ON failed_kafka_event (created_at);
