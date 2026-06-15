CREATE TYPE order_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED');

CREATE TABLE orders (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    user_email   VARCHAR(255) NOT NULL,
    status       order_status NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(12,2) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   UUID           NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(10,2)  NOT NULL
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status  ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
