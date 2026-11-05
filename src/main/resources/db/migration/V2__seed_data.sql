-- Initial data for development and demos

-- Development administrator: login admin@shoplite.local, password admin12345
-- (local use only; change it on any real server)
INSERT INTO users (email, password_hash, full_name, role)
VALUES ('admin@shoplite.local', '$2a$10$2mvnUUQPhElfc5v53crfFO2D6YL0ION5s6GEJSjclRDg2deZhCiN.', 'Administrator', 'ADMIN');

INSERT INTO categories (name) VALUES ('Kitchen'), ('Cleaning'), ('Bathroom'), ('Textiles');

INSERT INTO products (name, description, price, stock, category_id) VALUES
('Electric kettle 1.7 L', 'Stainless steel body, 2200 W, automatic shut-off.', 899.00, 4, (SELECT id FROM categories WHERE name = 'Kitchen')),
('Saucepan set, 3 pcs', '1.5 / 2.5 / 3.5 L saucepans with lids, induction compatible.', 1650.00, 6, (SELECT id FROM categories WHERE name = 'Kitchen')),
('Frying pan 26 cm', 'Non-stick coating, detachable handle.', 720.00, 10, (SELECT id FROM categories WHERE name = 'Kitchen')),
('Bamboo cutting board', '38 x 28 cm, with a juice groove.', 310.00, 15, (SELECT id FROM categories WHERE name = 'Kitchen')),
('Spin mop', 'Microfiber head, 10 L bucket with spinner.', 980.00, 5, (SELECT id FROM categories WHERE name = 'Cleaning')),
('Microfiber cloths, 5 pcs', '30 x 30 cm, for glass and furniture.', 185.00, 30, (SELECT id FROM categories WHERE name = 'Cleaning')),
('Pedal bin 20 L', 'Pedal bin with a soft-close lid.', 640.00, 3, (SELECT id FROM categories WHERE name = 'Cleaning')),
('Bath mat', 'Non-slip backing, 50 x 80 cm.', 420.00, 8, (SELECT id FROM categories WHERE name = 'Bathroom')),
('Soap dispenser', 'Ceramic, 300 ml.', 260.00, 12, (SELECT id FROM categories WHERE name = 'Bathroom')),
('Shower curtain', 'Water-repellent fabric, 180 x 200 cm, with rings.', 390.00, 0, (SELECT id FROM categories WHERE name = 'Bathroom')),
('Towel set, 3 pcs', 'Cotton 450 g/m2: 30 x 50, 50 x 90, 70 x 140 cm.', 760.00, 9, (SELECT id FROM categories WHERE name = 'Textiles')),
('Fleece blanket', '150 x 200 cm, grey.', 540.00, 7, (SELECT id FROM categories WHERE name = 'Textiles')),
('Bed linen set', 'Double, sateen, 4 pieces.', 1890.00, 4, (SELECT id FROM categories WHERE name = 'Textiles'));
