CREATE TABLE lots (
    id                   BIGSERIAL PRIMARY KEY,
    seller_id            BIGINT       NOT NULL REFERENCES users (id),
    title                VARCHAR(255) NOT NULL,
    description          TEXT         NOT NULL,
    category_id          BIGINT       NOT NULL REFERENCES categories (id),
    starting_price_cents BIGINT       NOT NULL CHECK (starting_price_cents > 0),
    min_increment_cents  BIGINT       NOT NULL CHECK (min_increment_cents > 0),
    reserve_price_cents  BIGINT       NULL CHECK (reserve_price_cents >= starting_price_cents),
    status               VARCHAR(32)  NOT NULL,
    scheduled_start_at   TIMESTAMPTZ,
    scheduled_end_at     TIMESTAMPTZ,
    actual_end_at        TIMESTAMPTZ,
    current_price_cents  BIGINT       NOT NULL DEFAULT 0,
    bid_count            INT          NOT NULL DEFAULT 0,
    winning_bid_id       BIGINT,
    extension_count      INT          NOT NULL DEFAULT 0,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL
);

CREATE TABLE watches (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    lot_id     BIGINT      NOT NULL REFERENCES lots (id),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, lot_id)
);

CREATE INDEX idx_lots_status_end ON lots (status, scheduled_end_at);
CREATE INDEX idx_lots_category ON lots (category_id) WHERE status = 'LIVE';
CREATE INDEX idx_lots_seller ON lots (seller_id);
