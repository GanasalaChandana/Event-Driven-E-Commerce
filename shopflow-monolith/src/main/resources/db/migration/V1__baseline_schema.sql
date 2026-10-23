-- Baseline schema for the monolith (users, catalogue, inventory, orders).
--
-- Until now the monolith let Hibernate create/alter tables (ddl-auto: update). The live database was built
-- that way, so every statement here is idempotent ("IF NOT EXISTS"): on that existing database this script
-- changes nothing but adds a few missing indexes, and on a brand-new database it creates the whole schema.
-- From here on, schema changes ship as new V<n>__*.sql files and Hibernate only *validates* them.
--
-- No seed data on purpose: the microservices' demo rows must never land in the monolith's database.

-- ---------------------------------------------------------------- users
CREATE TABLE IF NOT EXISTS users (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS addresses (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    line1       VARCHAR(150) NOT NULL,
    line2       VARCHAR(150),
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20)  NOT NULL,
    country     VARCHAR(2)   NOT NULL,
    phone       VARCHAR(30),
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_addresses_user ON addresses(user_id);

-- ------------------------------------------------------------ catalogue
CREATE TABLE IF NOT EXISTS categories (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    id          UUID           PRIMARY KEY,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    price       NUMERIC(10, 2) NOT NULL,
    sku         VARCHAR(100)   NOT NULL UNIQUE,
    category_id UUID           REFERENCES categories(id) ON DELETE SET NULL,
    image_url   VARCHAR(500),
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP      NOT NULL,
    updated_at  TIMESTAMP      NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_active   ON products(active);

-- ------------------------------------------------------------ inventory
CREATE TABLE IF NOT EXISTS inventory (
    id                UUID         PRIMARY KEY,
    product_id        UUID         NOT NULL UNIQUE,
    product_name      VARCHAR(255) NOT NULL,
    quantity          INT          NOT NULL DEFAULT 0,
    reserved_quantity INT          NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS stock_reservations (
    id          UUID        PRIMARY KEY,
    order_id    UUID        NOT NULL,
    product_id  UUID        NOT NULL,
    quantity    INT         NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT uk_reservation_order_product UNIQUE (order_id, product_id)
);
CREATE INDEX IF NOT EXISTS idx_stock_reservations_order_id ON stock_reservations(order_id);

-- --------------------------------------------------------------- orders
CREATE TABLE IF NOT EXISTS orders (
    id                UUID          PRIMARY KEY,
    user_id           UUID          NOT NULL,
    user_email        VARCHAR(255)  NOT NULL,
    status            VARCHAR(50)   NOT NULL,
    total_amount      NUMERIC(12,2) NOT NULL,
    payment_intent_id VARCHAR(100),
    ship_name         VARCHAR(100),
    ship_line1        VARCHAR(150),
    ship_line2        VARCHAR(150),
    ship_city         VARCHAR(100),
    ship_state        VARCHAR(100),
    ship_postal_code  VARCHAR(20),
    ship_country      VARCHAR(2),
    ship_phone        VARCHAR(30),
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL
);

-- Columns added after the first release, for databases that predate them
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ship_name         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ship_line1        VARCHAR(150),
    ADD COLUMN IF NOT EXISTS ship_line2        VARCHAR(150),
    ADD COLUMN IF NOT EXISTS ship_city         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ship_state        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ship_postal_code  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ship_country      VARCHAR(2),
    ADD COLUMN IF NOT EXISTS ship_phone        VARCHAR(30);
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status  ON orders(status);

CREATE TABLE IF NOT EXISTS order_items (
    id           UUID           PRIMARY KEY,
    order_id     UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   UUID           NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   NUMERIC(10,2)  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
