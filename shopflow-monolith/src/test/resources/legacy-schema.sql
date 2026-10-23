-- The monolith's database as Hibernate's "ddl-auto: update" left it before Flyway: varchar(255) defaults,
-- timestamp(6), Hibernate-style constraint names, no indexes, and an older orders table that has no
-- payment/shipping columns — plus a little data that must survive the migration.
create table users (id uuid not null, created_at timestamp(6) not null, email varchar(255) not null, name varchar(100) not null, password varchar(255) not null, role varchar(20) not null check (role in ('USER','ADMIN')), updated_at timestamp(6) not null, primary key (id));
alter table users add constraint uk_users_email unique (email);
create table categories (id uuid not null, created_at timestamp(6) not null, description TEXT, name varchar(100) not null, primary key (id));
alter table categories add constraint uk_categories_name unique (name);
create table products (active boolean not null, price numeric(10,2) not null, created_at timestamp(6) not null, updated_at timestamp(6) not null, category_id uuid, id uuid not null, sku varchar(100) not null, image_url varchar(500), name varchar(255) not null, description TEXT, primary key (id));
alter table products add constraint uk_products_sku unique (sku);
alter table products add constraint fk_products_category foreign key (category_id) references categories;
create table inventory (quantity integer not null, reserved_quantity integer not null, updated_at timestamp(6) not null, id uuid not null, product_id uuid not null, product_name varchar(255) not null, primary key (id));
alter table inventory add constraint uk_inventory_product unique (product_id);
create table stock_reservations (quantity integer not null, created_at timestamp(6) not null, updated_at timestamp(6) not null, id uuid not null, order_id uuid not null, product_id uuid not null, status varchar(20) not null check (status in ('RESERVED','COMMITTED','RELEASED')), primary key (id));
alter table stock_reservations add constraint uk_reservation_order_product unique (order_id, product_id);
create table orders (total_amount numeric(12,2) not null, created_at timestamp(6) not null, updated_at timestamp(6) not null, id uuid not null, user_id uuid not null, user_email varchar(255) not null, status VARCHAR(50) not null, primary key (id));
create table order_items (quantity integer not null, unit_price numeric(10,2) not null, id uuid not null, order_id uuid not null, product_id uuid not null, product_name varchar(255) not null, primary key (id));
alter table order_items add constraint fk_order_items_order foreign key (order_id) references orders;

insert into users values ('22222222-2222-2222-2222-222222222222', now(), 'legacy@shopflow.test', 'Legacy User', 'x', 'USER', now());
insert into categories values (gen_random_uuid(), now(), 'Old category', 'Electronics');
insert into orders values (99.99, now(), now(), '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'legacy@shopflow.test', 'CONFIRMED');
insert into order_items values (1, 99.99, gen_random_uuid(), '11111111-1111-1111-1111-111111111111', gen_random_uuid(), 'Wireless Headphones');
