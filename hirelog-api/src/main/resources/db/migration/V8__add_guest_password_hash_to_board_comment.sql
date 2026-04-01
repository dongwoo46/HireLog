ALTER TABLE board
    ADD COLUMN guest_password_hash VARCHAR(100);

ALTER TABLE comment
    ADD COLUMN guest_password_hash VARCHAR(100);

