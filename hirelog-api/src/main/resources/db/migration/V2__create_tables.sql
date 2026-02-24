-- ============================================================
-- 1. member  (VersionedEntity: created_at, updated_at, version)
-- ============================================================
CREATE TABLE member
(
    id                  BIGSERIAL    PRIMARY KEY,
    email               VARCHAR(255) NOT NULL,
    username            VARCHAR(100) NOT NULL,
    current_position_id BIGINT,
    career_years        INT,
    summary             VARCHAR(1000),
    role                VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0
);

-- ============================================================
-- 2. member_oauth_account  (BaseEntity: created_at, updated_at)
-- ============================================================
CREATE TABLE member_oauth_account
(
    id               BIGSERIAL    PRIMARY KEY,
    member_id        BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_oauth_member FOREIGN KEY (member_id) REFERENCES member (id)
);

-- ============================================================
-- 3. company  (BaseEntity)
-- ============================================================
CREATE TABLE company
(
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    source          VARCHAR(30)  NOT NULL,
    external_id     VARCHAR(100),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

-- ============================================================
-- 4. brand  (BaseEntity)
-- ============================================================
CREATE TABLE brand
(
    id                  BIGSERIAL    PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    normalized_name     VARCHAR(100) NOT NULL,
    company_id          BIGINT,
    verification_status VARCHAR(30)  NOT NULL,
    source              VARCHAR(20)  NOT NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL
);

-- ============================================================
-- 5. position_category  (BaseEntity)
-- ============================================================
CREATE TABLE position_category
(
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

-- ============================================================
-- 6. position  (BaseEntity)
-- ============================================================
CREATE TABLE position
(
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    category_id     BIGINT       NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT fk_position_category FOREIGN KEY (category_id) REFERENCES position_category (id)
);

-- ============================================================
-- 7. company_candidate  (VersionedEntity)
-- ============================================================
CREATE TABLE company_candidate
(
    id               BIGSERIAL        PRIMARY KEY,
    jd_summary_id    BIGINT           NOT NULL,
    brand_id         BIGINT           NOT NULL,
    candidate_name   VARCHAR(200)     NOT NULL,
    normalized_name  VARCHAR(200)     NOT NULL,
    source           VARCHAR(30)      NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL,
    status           VARCHAR(30)      NOT NULL,
    created_at       TIMESTAMP        NOT NULL,
    updated_at       TIMESTAMP        NOT NULL,
    version          BIGINT           NOT NULL DEFAULT 0
);

-- ============================================================
-- 8. job_snapshot  (BaseEntity)
-- ============================================================
CREATE TABLE job_snapshot
(
    id                      BIGSERIAL   PRIMARY KEY,
    brand_id                BIGINT,
    position_id             BIGINT,
    source_type             VARCHAR(30) NOT NULL,
    source_url              VARCHAR(1000),
    canonical_sections      JSONB       NOT NULL,
    core_text               TEXT        NOT NULL,
    recruitment_period_type VARCHAR(30) NOT NULL,
    opened_date             DATE,
    closed_date             DATE,
    canonical_hash          VARCHAR(64) NOT NULL,
    sim_hash                BIGINT      NOT NULL,
    created_at              TIMESTAMP   NOT NULL,
    updated_at              TIMESTAMP   NOT NULL
);

-- ============================================================
-- 9. jd_summary_processing  (VersionedEntity, UUID PK)
-- ============================================================
CREATE TABLE jd_summary_processing
(
    id                    UUID        PRIMARY KEY,
    status                VARCHAR(30) NOT NULL,
    job_snapshot_id       BIGINT,
    job_summary_id        BIGINT,
    llm_result_json       TEXT,
    command_brand_name    VARCHAR(255),
    command_position_name VARCHAR(255),
    duplicate_reason      VARCHAR(50),
    error_code            VARCHAR(100),
    error_message         TEXT,
    created_at            TIMESTAMP   NOT NULL,
    updated_at            TIMESTAMP   NOT NULL,
    version               BIGINT      NOT NULL DEFAULT 0
);

-- ============================================================
-- 10. job_summary  (VersionedEntity)
-- ============================================================
CREATE TABLE job_summary
(
    id                                  BIGSERIAL    PRIMARY KEY,
    job_snapshot_id                     BIGINT       NOT NULL,
    brand_id                            BIGINT       NOT NULL,
    brand_name                          VARCHAR(200) NOT NULL,
    company_id                          BIGINT,
    company_name                        VARCHAR(200),
    position_id                         BIGINT       NOT NULL,
    position_name                       VARCHAR(200) NOT NULL,
    brand_position_id                   BIGINT       NOT NULL,
    brand_position_name                 VARCHAR(300) NOT NULL,
    position_category_id                BIGINT       NOT NULL,
    position_category_name              VARCHAR(100) NOT NULL,
    career_type                         VARCHAR(20)  NOT NULL,
    career_years                        VARCHAR(50),
    summary_text                        TEXT         NOT NULL,
    responsibilities                    TEXT         NOT NULL,
    required_qualifications             TEXT         NOT NULL,
    preferred_qualifications            TEXT,
    tech_stack                          VARCHAR(1000),
    recruitment_process                 TEXT,
    -- JobSummaryInsight (embedded)
    ideal_candidate                     TEXT,
    must_have_signals                   TEXT,
    preparation_focus                   TEXT,
    transferable_strengths_and_gap_plan TEXT,
    proof_points_and_metrics            TEXT,
    story_angles                        TEXT,
    key_challenges                      TEXT,
    technical_context                   TEXT,
    questions_to_ask                    TEXT,
    considerations                      TEXT,
    -- LLM metadata
    llm_provider                        VARCHAR(30)  NOT NULL,
    llm_model                           VARCHAR(50)  NOT NULL,
    source_url                          VARCHAR(2000),
    is_active                           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                          TIMESTAMP    NOT NULL,
    updated_at                          TIMESTAMP    NOT NULL,
    version                             BIGINT       NOT NULL DEFAULT 0
);

-- ============================================================
-- 11. job_summary_request  (BaseEntity)
-- ============================================================
CREATE TABLE job_summary_request
(
    id             BIGSERIAL    PRIMARY KEY,
    member_id      BIGINT       NOT NULL,
    request_id     VARCHAR(100) NOT NULL,
    job_summary_id BIGINT,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);

-- ============================================================
-- 12. job_summary_review  (BaseEntity)
-- ============================================================
CREATE TABLE job_summary_review
(
    id                  BIGSERIAL   PRIMARY KEY,
    job_summary_id      BIGINT      NOT NULL,
    member_id           BIGINT      NOT NULL,
    hiring_stage        VARCHAR(30) NOT NULL,
    is_anonymous        BOOLEAN     NOT NULL,
    difficulty_rating   INT         NOT NULL CHECK (difficulty_rating >= 1 AND difficulty_rating <= 10),
    satisfaction_rating INT         NOT NULL CHECK (satisfaction_rating >= 1 AND satisfaction_rating <= 10),
    experience_comment  TEXT        NOT NULL,
    interview_tip       TEXT,
    deleted             BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL,
    CONSTRAINT uk_review_job_member UNIQUE (job_summary_id, member_id)
);

-- ============================================================
-- 13. brand_position  (BaseEntity)
-- ============================================================
CREATE TABLE brand_position
(
    id           BIGSERIAL    PRIMARY KEY,
    brand_id     BIGINT       NOT NULL,
    position_id  BIGINT       NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    source       VARCHAR(20)  NOT NULL,
    approved_at  TIMESTAMP,
    approved_by  BIGINT,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT uk_brand_position_brand_id_position_id UNIQUE (brand_id, position_id)
);

-- ============================================================
-- 14. member_job_summary  (BaseEntity)
-- ============================================================
CREATE TABLE member_job_summary
(
    id                     BIGSERIAL    PRIMARY KEY,
    member_id              BIGINT       NOT NULL,
    job_summary_id         BIGINT       NOT NULL,
    brand_name             VARCHAR(200) NOT NULL,
    position_name          VARCHAR(200) NOT NULL,
    brand_position_name    VARCHAR(300) NOT NULL,
    position_category_name VARCHAR(100) NOT NULL,
    save_type              VARCHAR(20)  NOT NULL,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP    NOT NULL
);

-- ============================================================
-- 15. member_job_summary_stage  (HiringStageRecord, no BaseEntity)
-- ============================================================
CREATE TABLE member_job_summary_stage
(
    id                    BIGSERIAL     PRIMARY KEY,
    member_job_summary_id BIGINT        NOT NULL,
    stage                 VARCHAR(30)   NOT NULL,
    note                  VARCHAR(2000) NOT NULL,
    result                VARCHAR(20),
    recorded_at           TIMESTAMP     NOT NULL,
    CONSTRAINT ux_member_job_summary_stage UNIQUE (member_job_summary_id, stage),
    CONSTRAINT fk_stage_record_member_job_summary FOREIGN KEY (member_job_summary_id) REFERENCES member_job_summary (id)
);

-- ============================================================
-- 16. cover_letter  (no BaseEntity)
-- ============================================================
CREATE TABLE cover_letter
(
    id                    BIGSERIAL PRIMARY KEY,
    member_job_summary_id BIGINT    NOT NULL,
    question              TEXT      NOT NULL,
    content               TEXT      NOT NULL,
    sort_order            INTEGER   NOT NULL DEFAULT 0,
    CONSTRAINT fk_cover_letter_member_job_summary FOREIGN KEY (member_job_summary_id) REFERENCES member_job_summary (id)
);

-- ============================================================
-- 17. member_company  (BaseEntity)
-- ============================================================
CREATE TABLE member_company
(
    id            BIGSERIAL   PRIMARY KEY,
    member_id     BIGINT      NOT NULL,
    company_id    BIGINT      NOT NULL,
    interest_type VARCHAR(20) NOT NULL DEFAULT 'FAVORITE',
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL
);

-- ============================================================
-- 18. member_brand  (BaseEntity)
-- ============================================================
CREATE TABLE member_brand
(
    id            BIGSERIAL   PRIMARY KEY,
    member_id     BIGINT      NOT NULL,
    brand_id      BIGINT      NOT NULL,
    interest_type VARCHAR(32) NOT NULL DEFAULT 'FAVORITE',
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL
);

-- ============================================================
-- 19. notification  (BaseEntity)
-- ============================================================
CREATE TABLE notification
(
    id             BIGSERIAL    PRIMARY KEY,
    member_id      BIGINT       NOT NULL,
    type           VARCHAR(50)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    message        TEXT,
    reference_type VARCHAR(50),
    reference_id   BIGINT,
    metadata       TEXT,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at        TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);

-- ============================================================
-- 20. user_request  (VersionedEntity)
-- ============================================================
CREATE TABLE user_request
(
    id           BIGSERIAL    PRIMARY KEY,
    member_id    BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    request_type VARCHAR(50)  NOT NULL,
    content      TEXT         NOT NULL,
    status       VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    resolved_at  TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0
);

-- ============================================================
-- 21. user_request_comment  (BaseEntity)
-- ============================================================
CREATE TABLE user_request_comment
(
    id              BIGSERIAL   PRIMARY KEY,
    user_request_id BIGINT      NOT NULL,
    writer_type     VARCHAR(30) NOT NULL,
    writer_id       BIGINT      NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,
    CONSTRAINT fk_comment_user_request FOREIGN KEY (user_request_id) REFERENCES user_request (id)
);

-- ============================================================
-- 22. outbox_event  (UUID PK, created_at only)
-- ============================================================
CREATE TABLE outbox_event
(
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    created_at     TIMESTAMP    NOT NULL
);

-- ============================================================
-- 23. processed_event  (composite PK, no BaseEntity)
-- ============================================================
CREATE TABLE processed_event
(
    event_id       VARCHAR(100) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMP    NOT NULL,
    PRIMARY KEY (event_id, consumer_group)
);

-- ============================================================
-- 24. failed_kafka_event  (BaseEntity)
-- ============================================================
CREATE TABLE failed_kafka_event
(
    id                BIGSERIAL    PRIMARY KEY,
    topic             VARCHAR(255) NOT NULL,
    partition_number  INT          NOT NULL,
    offset_number     BIGINT       NOT NULL,
    record_key        VARCHAR(500),
    record_value      TEXT,
    consumer_group    VARCHAR(255) NOT NULL,
    exception_class   VARCHAR(500) NOT NULL,
    exception_message TEXT,
    stack_trace       TEXT,
    retry_count       INT          NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'FAILED',
    failed_at         TIMESTAMP    NOT NULL,
    reprocessed_at    TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);
