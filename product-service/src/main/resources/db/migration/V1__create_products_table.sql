CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    price       NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    sku         VARCHAR(100)   NOT NULL UNIQUE,
    category_id UUID           REFERENCES categories(id) ON DELETE SET NULL,
    image_url   VARCHAR(500),
    active      BOOLEAN        NOT NULL DEFAULT true,
    created_at  TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category  ON products(category_id);
CREATE INDEX idx_products_sku       ON products(sku);
CREATE INDEX idx_products_active    ON products(active);

-- Seed categories
INSERT INTO categories (name, description) VALUES
    ('Electronics',  'Phones, laptops, accessories'),
    ('Clothing',     'Apparel and fashion'),
    ('Books',        'Physical and digital books'),
    ('Home & Garden','Furniture and home decor');
