CREATE TABLE addresses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_addresses_user ON addresses(user_id);
