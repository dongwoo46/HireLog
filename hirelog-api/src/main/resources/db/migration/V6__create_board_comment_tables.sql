-- board (게시글)
CREATE TABLE board
(
    id         BIGSERIAL    PRIMARY KEY,
    member_id  BIGINT       NOT NULL REFERENCES member (id),
    board_type VARCHAR(30)  NOT NULL,
    title      VARCHAR(300) NOT NULL,
    content    TEXT         NOT NULL,
    anonymous  BOOLEAN      NOT NULL DEFAULT false,
    deleted    BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_board_member           ON board (member_id);
CREATE INDEX idx_board_deleted_created  ON board (deleted, created_at);
CREATE INDEX idx_board_type             ON board (board_type);

-- board_like (게시글 좋아요)
CREATE TABLE board_like
(
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL REFERENCES member (id),
    board_id   BIGINT    NOT NULL REFERENCES board (id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_board_like_member_board UNIQUE (member_id, board_id)
);

CREATE INDEX idx_board_like_board   ON board_like (board_id);
CREATE INDEX idx_board_like_member  ON board_like (member_id);

-- comment (댓글)
CREATE TABLE comment
(
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT    NOT NULL REFERENCES board (id),
    member_id  BIGINT    NOT NULL REFERENCES member (id),
    content    TEXT      NOT NULL,
    anonymous  BOOLEAN   NOT NULL DEFAULT false,
    deleted    BOOLEAN   NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_comment_board   ON comment (board_id);
CREATE INDEX idx_comment_member  ON comment (member_id);

-- comment_like (댓글 좋아요)
CREATE TABLE comment_like
(
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL REFERENCES member (id),
    comment_id BIGINT    NOT NULL REFERENCES comment (id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_comment_like_member_comment UNIQUE (member_id, comment_id)
);

CREATE INDEX idx_comment_like_comment  ON comment_like (comment_id);
CREATE INDEX idx_comment_like_member   ON comment_like (member_id);

-- report: board_id, comment_id 컬럼 추가
ALTER TABLE report
    ADD COLUMN board_id   BIGINT REFERENCES board (id),
    ADD COLUMN comment_id BIGINT REFERENCES comment (id);

-- report CHECK constraint 재정의 (5개 대상)
ALTER TABLE report DROP CONSTRAINT chk_report_single_target;
ALTER TABLE report ADD CONSTRAINT chk_report_single_target CHECK (
    (CASE WHEN job_summary_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN job_summary_review_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN reported_member_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN board_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN comment_id IS NOT NULL THEN 1 ELSE 0 END) = 1
);

CREATE INDEX idx_report_board    ON report (board_id);
CREATE INDEX idx_report_comment  ON report (comment_id);

-- 중복 신고 방지 partial unique index
CREATE UNIQUE INDEX uq_report_reporter_board
    ON report (reporter_id, board_id) WHERE board_id IS NOT NULL;

CREATE UNIQUE INDEX uq_report_reporter_comment
    ON report (reporter_id, comment_id) WHERE comment_id IS NOT NULL;