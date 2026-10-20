-- PostgreSQL: run once before the updated application starts.
-- Safe to repeat: existing non-null statuses and dates are preserved.
-- Legacy created_at is migration time; original creation dates were not stored.
ALTER TABLE shipments
    ADD COLUMN IF NOT EXISTS status varchar(32) DEFAULT 'CREATED',
    ADD COLUMN IF NOT EXISTS created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivered_at timestamp with time zone;

UPDATE shipments SET status = 'CREATED' WHERE status IS NULL;
UPDATE shipments SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

ALTER TABLE shipments
    ALTER COLUMN status SET DEFAULT 'CREATED',
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL;
