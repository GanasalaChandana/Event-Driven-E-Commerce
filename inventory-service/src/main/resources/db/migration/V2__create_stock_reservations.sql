-- Per-order stock reservations: make reserve / commit / release idempotent
CREATE TABLE stock_reservations (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID        NOT NULL,
    product_id  UUID        NOT NULL,
    quantity    INT         NOT NULL CHECK (quantity > 0),
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uk_reservation_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
