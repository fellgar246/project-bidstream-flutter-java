CREATE TABLE bids (
    id                BIGSERIAL PRIMARY KEY,
    lot_id            BIGINT       NOT NULL REFERENCES lots (id),
    bidder_id         BIGINT       NOT NULL REFERENCES users (id),
    amount_cents      BIGINT       NOT NULL CHECK (amount_cents > 0),
    placed_at         TIMESTAMPTZ  NOT NULL,
    client_request_id VARCHAR(64)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    UNIQUE (lot_id, bidder_id, client_request_id)
);

CREATE INDEX idx_bids_lot_amount ON bids (lot_id, amount_cents DESC, id DESC);
CREATE INDEX idx_bids_bidder ON bids (bidder_id, placed_at DESC);
