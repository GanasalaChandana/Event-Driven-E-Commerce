CREATE TABLE inventory (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id        UUID           NOT NULL UNIQUE,
    product_name      VARCHAR(255)   NOT NULL,
    quantity          INT            NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INT            NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    updated_at        TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_product_id ON inventory(product_id);

-- Seed with some demo stock
INSERT INTO inventory (product_id, product_name, quantity) VALUES
    (gen_random_uuid(), 'Demo Product A', 100),
    (gen_random_uuid(), 'Demo Product B', 50),
    (gen_random_uuid(), 'Demo Product C', 0);
