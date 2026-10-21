-- The entity maps status as VARCHAR (EnumType.STRING); a native Postgres enum
-- rejects those inserts ("column status is of type order_status but expression
-- is of type character varying"). Convert the column to VARCHAR.
ALTER TABLE orders ALTER COLUMN status DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'PENDING';
DROP TYPE IF EXISTS order_status;
