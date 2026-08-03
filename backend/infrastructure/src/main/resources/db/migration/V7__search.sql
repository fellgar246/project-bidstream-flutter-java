ALTER TABLE lots
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('spanish', coalesce(title, '')), 'A') ||
            setweight(to_tsvector('spanish', coalesce(description, '')), 'B')
            ) STORED;

CREATE INDEX idx_lots_search ON lots USING GIN (search_vector);
