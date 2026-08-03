CREATE TABLE outbox_events (
    id               BIGSERIAL PRIMARY KEY,
    aggregate_type   VARCHAR(64)  NOT NULL,
    aggregate_id     BIGINT       NOT NULL,
    event_type       VARCHAR(64)  NOT NULL,
    payload          JSONB        NOT NULL,
    occurred_at      TIMESTAMPTZ  NOT NULL,
    published_at     TIMESTAMPTZ  NULL,
    attempts         INT          NOT NULL DEFAULT 0,
    last_error       TEXT         NULL
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (id) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    consumer     VARCHAR(64) NOT NULL,
    event_id     BIGINT      NOT NULL REFERENCES outbox_events (id),
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer, event_id)
);

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    type       VARCHAR(64)  NOT NULL,
    payload    JSONB        NOT NULL,
    read_at    TIMESTAMPTZ  NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notif_user_unread ON notifications (user_id, created_at DESC) WHERE read_at IS NULL;

CREATE TABLE invoices (
    id           BIGSERIAL PRIMARY KEY,
    lot_id       BIGINT       NOT NULL UNIQUE REFERENCES lots (id),
    buyer_id     BIGINT       NOT NULL REFERENCES users (id),
    seller_id    BIGINT       NOT NULL REFERENCES users (id),
    amount_cents BIGINT       NOT NULL,
    status       VARCHAR(32)  NOT NULL,
    issued_at    TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_lots_status_start ON lots (status, scheduled_start_at) WHERE status = 'SCHEDULED';
