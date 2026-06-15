# ShopFlow — Event-Driven E-Commerce Backend

A production-grade microservices backend where 5 independent services coordinate through Kafka events to process orders in real time — no direct service-to-service HTTP calls.

## Architecture

```
Client → API Gateway (JWT auth + routing)
              ↓
    ┌─────────┬──────────┬───────────┬──────────────┐
User Svc  Product Svc  Order Svc  Inventory Svc  Notification Svc
  :8081      :8082       :8083        :8084            :8085
    │           │           │             │                │
    └───────────┴───────────┴─────────────┴────────────────┘
                          Kafka Event Bus
                    order.created → inventory.reserved
                    inventory.reserved → order.confirmed
                    order.confirmed → email sent
```

## Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| API Gateway | 8080 | — | JWT validation, request routing |
| Eureka Server | 8761 | — | Service discovery |
| User Service | 8081 | users_db | Registration, login, JWT |
| Product Service | 8082 | products_db | Product & category CRUD |
| Order Service | 8083 | orders_db | Place orders, track status |
| Inventory Service | 8084 | inventory_db + Redis | Stock levels, reserve/release |
| Notification Service | 8085 | — | Email on order events |

## Event Flow

```
1. POST /api/v1/orders  →  Order saved as PENDING
                        →  Publishes: order.created

2. Inventory Service consumes order.created
   → Checks + reserves stock
   → Publishes: inventory.reserved  (or inventory.failed)

3. Order Service consumes inventory.reserved
   → Updates order to CONFIRMED
   → Publishes: order.confirmed

4. Notification Service consumes order.confirmed
   → Sends confirmation email to user
```

## Tech Stack

- **Java 17** + **Spring Boot 3.2**
- **Spring Cloud Gateway** — API gateway with JWT filter
- **Spring Cloud Netflix Eureka** — service discovery
- **Apache Kafka** — async event bus (Saga pattern)
- **PostgreSQL 16** — each service owns its database
- **Redis 7** — inventory stock cache
- **Flyway** — database migrations
- **JWT (JJWT 0.12)** — stateless authentication
- **Micrometer + Zipkin** — distributed tracing
- **Docker Compose** — local orchestration

## Running Locally

**Prerequisites:** Java 17, Maven 3.9+, Docker Desktop

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts: PostgreSQL (4 databases), Redis, Kafka, Zookeeper, Zipkin, Kafka UI

### 2. Start services (in order)

```bash
# Terminal 1
cd eureka-server && mvn spring-boot:run

# Terminal 2
cd user-service && mvn spring-boot:run

# Terminal 3
cd product-service && mvn spring-boot:run

# Terminal 4
cd order-service && mvn spring-boot:run

# Terminal 5
cd inventory-service && mvn spring-boot:run

# Terminal 6
cd notification-service && mvn spring-boot:run

# Terminal 7
cd api-gateway && mvn spring-boot:run
```

### 3. Verify everything is up

| URL | What you see |
|---|---|
| http://localhost:8761 | Eureka dashboard — all services registered |
| http://localhost:8090 | Kafka UI — browse topics and messages |
| http://localhost:9411 | Zipkin — distributed traces |

## API Reference

### Auth

```
POST /api/v1/auth/register
{
  "name": "Chandana",
  "email": "chandana@example.com",
  "password": "secret123"
}

POST /api/v1/auth/login
{
  "email": "chandana@example.com",
  "password": "secret123"
}
→ returns { "token": "eyJ..." }
```

### Products

```
GET    /api/v1/products          # public
GET    /api/v1/products/{id}     # public
POST   /api/v1/products          # requires JWT (ADMIN)
PUT    /api/v1/products/{id}     # requires JWT (ADMIN)
DELETE /api/v1/products/{id}     # requires JWT (ADMIN)
```

### Orders

```
POST /api/v1/orders              # place an order (requires JWT)
GET  /api/v1/orders              # list my orders (requires JWT)
GET  /api/v1/orders/{id}         # get order detail (requires JWT)
```

### Inventory

```
GET  /api/v1/inventory/{productId}   # check stock
POST /api/v1/inventory               # add stock (ADMIN)
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | (dev default) | Must be 256-bit min in production |
| `MAIL_HOST` | smtp.gmail.com | SMTP host |
| `MAIL_USERNAME` | — | Sender email |
| `MAIL_PASSWORD` | — | App password (not account password) |

## Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| `order.created` | Order Service | Inventory Service |
| `inventory.reserved` | Inventory Service | Order Service |
| `inventory.failed` | Inventory Service | Order Service |
| `order.confirmed` | Order Service | Notification Service |
| `order.cancelled` | Order Service | Notification Service |

## Project Structure

```
shopflow/
├── docker-compose.yml
├── scripts/init-db.sql
├── eureka-server/
├── api-gateway/
├── user-service/
├── product-service/
├── order-service/
├── inventory-service/
└── notification-service/
```

Each service follows the same internal layout:
```
src/main/java/com/shopflow/<service>/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── event/       ← Kafka event POJOs
├── config/
└── exception/
```
