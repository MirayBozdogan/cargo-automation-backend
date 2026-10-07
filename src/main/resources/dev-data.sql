-- Sample data only: prices are fictional and do not represent a carrier tariff.
-- Prevent this seed from modifying the original project database.
-- A wrong database aborts initialization with division by zero.
SELECT 1 / CASE WHEN current_database() = 'cargo_automation_dev' THEN 1 ELSE 0 END;

INSERT INTO cities (name)
SELECT 'İstanbul' WHERE NOT EXISTS (SELECT 1 FROM cities WHERE name = 'İstanbul');
INSERT INTO cities (name)
SELECT 'Ankara' WHERE NOT EXISTS (SELECT 1 FROM cities WHERE name = 'Ankara');
INSERT INTO cities (name)
SELECT 'İzmir' WHERE NOT EXISTS (SELECT 1 FROM cities WHERE name = 'İzmir');

INSERT INTO districts (name, city_id)
SELECT d.name, c.id FROM (VALUES
    ('Kadıköy', 'İstanbul'), ('Üsküdar', 'İstanbul'),
    ('Çankaya', 'Ankara'), ('Keçiören', 'Ankara'),
    ('Konak', 'İzmir'), ('Karşıyaka', 'İzmir')
) AS d(name, city_name)
JOIN cities c ON c.name = d.city_name
WHERE NOT EXISTS (SELECT 1 FROM districts old WHERE old.name = d.name AND old.city_id = c.id);

INSERT INTO prices (min_desi, max_desi, price)
SELECT p.min_desi, p.max_desi, p.price FROM (VALUES
    (0.00, 1.00, 60.00), (1.00, 3.00, 80.00),
    (3.00, 5.00, 100.00), (5.00, 10.00, 140.00),
    (10.00, 20.00, 200.00), (20.00, 50.00, 300.00),
    (50.00, 100.00, 500.00)
) AS p(min_desi, max_desi, price)
WHERE NOT EXISTS (SELECT 1 FROM prices old
    WHERE old.min_desi = p.min_desi AND old.max_desi = p.max_desi);
