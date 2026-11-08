-- Sample data only: prices are fictional and do not represent a carrier tariff.
-- Prevent this seed from modifying the original project database.
-- A wrong database aborts initialization with division by zero.
SELECT 1 / CASE WHEN current_database() = 'cargo_automation_dev' THEN 1 ELSE 0 END;

INSERT INTO prices (min_desi, max_desi, price)
SELECT p.min_desi, p.max_desi, p.price FROM (VALUES
    (0.00, 1.00, 60.00), (1.00, 3.00, 80.00),
    (3.00, 5.00, 100.00), (5.00, 10.00, 140.00),
    (10.00, 20.00, 200.00), (20.00, 50.00, 300.00),
    (50.00, 100.00, 500.00)
) AS p(min_desi, max_desi, price)
WHERE NOT EXISTS (SELECT 1 FROM prices old
    WHERE old.min_desi = p.min_desi AND old.max_desi = p.max_desi);
