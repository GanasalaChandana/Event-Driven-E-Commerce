-- Creates all 4 databases in the single PostgreSQL container
-- Runs automatically on first startup via docker-entrypoint-initdb.d

CREATE DATABASE users_db;
CREATE DATABASE products_db;
CREATE DATABASE orders_db;
CREATE DATABASE inventory_db;

-- Grant all privileges to the shopflow user
GRANT ALL PRIVILEGES ON DATABASE users_db TO shopflow;
GRANT ALL PRIVILEGES ON DATABASE products_db TO shopflow;
GRANT ALL PRIVILEGES ON DATABASE orders_db TO shopflow;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO shopflow;
