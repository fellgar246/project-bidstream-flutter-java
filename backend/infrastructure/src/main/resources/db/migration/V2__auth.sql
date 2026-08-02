CREATE TABLE user_roles (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    role    VARCHAR(50) NOT NULL CHECK (role IN ('BUYER', 'SELLER', 'ADMIN')),
    UNIQUE (user_id, role)
);

CREATE TABLE refresh_tokens (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users (id),
    token_hash     VARCHAR(64) NOT NULL UNIQUE,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ NULL,
    replaced_by_id BIGINT NULL REFERENCES refresh_tokens (id),
    user_agent     VARCHAR(512),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
