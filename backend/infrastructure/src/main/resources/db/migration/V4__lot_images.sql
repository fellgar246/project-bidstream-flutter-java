CREATE TABLE lot_images (
    id             BIGSERIAL PRIMARY KEY,
    lot_id         BIGINT       NOT NULL REFERENCES lots (id) ON DELETE CASCADE,
    storage_key    VARCHAR(512) NOT NULL UNIQUE,
    thumbnail_key  VARCHAR(512),
    position       INT          NOT NULL,
    content_type   VARCHAR(64)  NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    UNIQUE (lot_id, position)
);

CREATE INDEX idx_lot_images_lot_id ON lot_images (lot_id);
CREATE INDEX idx_lot_images_pending_created ON lot_images (status, created_at)
    WHERE status = 'PENDING';
