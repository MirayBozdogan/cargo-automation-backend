-- Run with psql -v ON_ERROR_STOP=1 -v test_schema=unique_test_schema -f this-file.sql
-- The test schema and its fixture data are rolled back, including on disconnect.
SELECT 1 / CASE WHEN current_database() = 'cargo_automation_dev' THEN 1 ELSE 0 END;
BEGIN;
CREATE SCHEMA :"test_schema";
SET LOCAL search_path TO :"test_schema";
CREATE TABLE shipments (id integer PRIMARY KEY, barcode varchar(12), deleted boolean);
INSERT INTO shipments VALUES (1, 'LEGACY000001', false), (2, 'LEGACY000002', true);

\ir ../../main/resources/db/manual/001_shipment_lifecycle.sql
SELECT 1 / CASE WHEN
    (SELECT count(*) FROM shipments WHERE status = 'CREATED' AND created_at IS NOT NULL AND delivered_at IS NULL) = 2
    AND (SELECT barcode = 'LEGACY000002' AND deleted FROM shipments WHERE id = 2)
    THEN 1 ELSE 0 END;

UPDATE shipments SET status = 'ACCEPTED', created_at = '2026-01-01T10:00:00Z' WHERE id = 1;
\ir ../../main/resources/db/manual/001_shipment_lifecycle.sql
SELECT 1 / CASE WHEN (SELECT status = 'ACCEPTED' AND created_at = '2026-01-01T10:00:00Z'::timestamptz FROM shipments WHERE id = 1)
    THEN 1 ELSE 0 END;

-- Partial migration: null rows must also be repaired.
ALTER TABLE shipments ALTER COLUMN status DROP NOT NULL, ALTER COLUMN created_at DROP NOT NULL;
INSERT INTO shipments (id, barcode, deleted, status, created_at) VALUES (3, 'PARTIAL00003', false, NULL, NULL);
\ir ../../main/resources/db/manual/001_shipment_lifecycle.sql
SELECT 1 / CASE WHEN (SELECT status = 'CREATED' AND created_at IS NOT NULL FROM shipments WHERE id = 3) THEN 1 ELSE 0 END;
INSERT INTO shipments (id,barcode,deleted) VALUES (4,'NEW000000004',false);
SELECT 1 / CASE WHEN (SELECT status = 'CREATED' AND created_at IS NOT NULL AND delivered_at IS NULL FROM shipments WHERE id = 4)
    THEN 1 ELSE 0 END;
ROLLBACK;
