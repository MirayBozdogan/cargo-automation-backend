-- Run with the intended schema on search_path. Existing shipments remain unassigned.
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS courier_id integer;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_shipments_courier' AND conrelid = 'shipments'::regclass
    ) THEN
        ALTER TABLE shipments ADD CONSTRAINT fk_shipments_courier
            FOREIGN KEY (courier_id) REFERENCES users(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_shipments_courier ON shipments(courier_id);
