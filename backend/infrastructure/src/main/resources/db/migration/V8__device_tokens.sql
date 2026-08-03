CREATE TABLE device_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id),
    token        VARCHAR(512) NOT NULL UNIQUE,
    platform     VARCHAR(32)  NOT NULL,
    last_seen_at TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);
