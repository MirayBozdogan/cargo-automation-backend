\set ON_ERROR_STOP on
BEGIN;
CREATE SCHEMA :"test_schema";
SET LOCAL search_path TO :"test_schema";
CREATE TABLE users (id integer PRIMARY KEY);
CREATE TABLE shipments (
    id integer PRIMARY KEY, barcode varchar(12), status varchar(32),
    created_at timestamptz, delivered_at timestamptz, deleted boolean
);
INSERT INTO users VALUES (7);
INSERT INTO shipments VALUES
    (1,'TEST00000001','CREATED','2026-01-01T10:00:00Z',null,false),
    (2,'TEST00000002','DELIVERED','2026-01-01T10:00:00Z','2026-01-02T10:00:00Z',true);
CREATE TEMP TABLE before_courier AS SELECT to_jsonb(s) AS row FROM shipments s;
\ir ../../main/resources/db/manual/002_shipment_courier.sql
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM shipments WHERE courier_id IS NOT NULL) THEN
        RAISE EXCEPTION 'Legacy shipments must stay unassigned';
    END IF;
    IF EXISTS ((SELECT to_jsonb(s) - 'courier_id' FROM shipments s EXCEPT SELECT row FROM before_courier)
        UNION ALL (SELECT row FROM before_courier EXCEPT SELECT to_jsonb(s) - 'courier_id' FROM shipments s)) THEN
        RAISE EXCEPTION 'Existing shipment data changed';
    END IF;
END $$;
UPDATE shipments SET courier_id=7 WHERE id=1;
\ir ../../main/resources/db/manual/002_shipment_courier.sql
DO $$ BEGIN
    IF (SELECT courier_id FROM shipments WHERE id=1) <> 7 THEN
        RAISE EXCEPTION 'Existing assignment lost on repeated migration';
    END IF;
    BEGIN
        UPDATE shipments SET courier_id=999 WHERE id=1;
        RAISE EXCEPTION 'Expected foreign key rejection';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
END $$;
ROLLBACK;
