-- Delivery address copied onto the order when it is placed (null for older orders).
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS ship_name        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ship_line1       VARCHAR(150),
    ADD COLUMN IF NOT EXISTS ship_line2       VARCHAR(150),
    ADD COLUMN IF NOT EXISTS ship_city        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ship_state       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ship_postal_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ship_country     VARCHAR(2),
    ADD COLUMN IF NOT EXISTS ship_phone       VARCHAR(30);
