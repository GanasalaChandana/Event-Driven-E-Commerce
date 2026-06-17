# ShopFlow — Event-Driven E-Commerce Backend

A production-grade e-commerce backend built as a Spring Boot monolith that combines 4 domain services (User, Product, Order, Inventory) into a single deployable unit using `build-helper-maven-plugin`. Services communicate asynchronously via Kafka events with graceful degradation when Kafka is unavailable.

**Live API:** https://event-driven-e-commerce.onrender.com

---

## Architecture

```
Client
  │
  ▼
shopflow-monolith (Spring Boot 3.x, port 10000)
  ├── UserService     — registration, login, JWT
  ├── ProductService  — product & category CRUD
  ├── OrderService    — place orders, track status
  └── InventoryService — stock management
         │
         ▼
   Apache Kafka (async event bus — Saga pattern)
   order.created → inventory.reserved → order.confirmed
```

### Event Flow

```
1. POST /api/v1/orders
   → Order saved as PENDING
   → Publishes: order.created

2. Inventory Service consumes order.created
   → Checks + reserves stock
   → Publishes: inventory.reserved  (or inventory.failed)

3. Order Service consumes inventory.reserved
   → Updates order to CONFIRMED
   → Publishes: order.confirmed
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 + JWT (JJWT 0.12) |
| Database | PostgreSQL |
| Cache | Redis (inventory stock cache) |
| Messaging | Apache Kafka |
| Deployment | Docker + Render.com |

---

## API Reference

**Base URL:** `https://event-driven-e-commerce.onrender.com`

> The free Render instance spins down after inactivity. First request may take 30–60 seconds to wake up.

---

### Authentication

#### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Chandana",
  "email": "chandana@example.com",
  "password": "secret123"
}
```

**Response — 201 Created**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "type": "Bearer",
  "user": {
    "id": "a1b2c3d4-...",
    "name": "Chandana",
    "email": "chandana@example.com",
    "role": "USER"
  }
}
```

---

#### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "chandana@example.com",
  "password": "secret123"
}
```

**Response — 200 OK**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "type": "Bearer",
  "user": {
    "id": "a1b2c3d4-...",
    "name": "Chandana",
    "email": "chandana@example.com",
    "role": "USER"
  }
}
```

> A default admin account is seeded on startup:
> **Email:** `admin@shopflow.com` | **Password:** `admin123`

---

### Products

#### List Products (public)

```http
GET /api/v1/products?page=0&size=20&sort=createdAt,desc
```

**Response — 200 OK**
```json
{
  "content": [
    {
      "id": "ee04ec8d-a1a3-4941-9f78-6dd19dfdc381",
      "name": "Wireless Headphones",
      "description": "High quality noise-cancelling headphones",
      "price": 99.99,
      "sku": "WH-001",
      "category": null,
      "imageUrl": null,
      "active": true,
      "createdAt": "2026-06-17T03:26:27.869374599"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

**Query parameters:**
| Param | Description |
|---|---|
| `page` | Page number (0-indexed) |
| `size` | Items per page (default 20) |
| `q` | Full-text search by name/description |
| `categoryId` | Filter by category UUID |

---

#### Get Product by ID (public)

```http
GET /api/v1/products/{id}
```

**Response — 200 OK**
```json
{
  "id": "ee04ec8d-a1a3-4941-9f78-6dd19dfdc381",
  "name": "Wireless Headphones",
  "description": "High quality noise-cancelling headphones",
  "price": 99.99,
  "sku": "WH-001",
  "category": null,
  "imageUrl": null,
  "active": true,
  "createdAt": "2026-06-17T03:26:27.869374599"
}
```

---

#### Create Product (Admin only)

```http
POST /api/v1/products
Content-Type: application/json
Authorization: Bearer <admin-token>

{
  "name": "Wireless Headphones",
  "description": "High quality noise-cancelling headphones",
  "price": 99.99,
  "sku": "WH-001",
  "categoryId": null,
  "imageUrl": null
}
```

**Required fields:** `name`, `price`, `sku`

**Response — 201 Created**
```json
{
  "id": "ee04ec8d-a1a3-4941-9f78-6dd19dfdc381",
  "name": "Wireless Headphones",
  "price": 99.99,
  "sku": "WH-001",
  "active": true,
  "createdAt": "2026-06-17T03:26:27.869374599"
}
```

---

#### Update Product (Admin only)

```http
PUT /api/v1/products/{id}
Content-Type: application/json
Authorization: Bearer <admin-token>

{
  "name": "Wireless Headphones Pro",
  "description": "Updated description",
  "price": 129.99,
  "sku": "WH-001-PRO"
}
```

**Response — 200 OK** — updated product object

---

#### Delete Product (Admin only)

```http
DELETE /api/v1/products/{id}
Authorization: Bearer <admin-token>
```

**Response — 204 No Content**

---

### Inventory

#### Check Stock (public)

```http
GET /api/v1/inventory/{productId}
```

**Response — 200 OK**
```json
{
  "id": "...",
  "productId": "ee04ec8d-a1a3-4941-9f78-6dd19dfdc381",
  "productName": "Wireless Headphones",
  "quantityAvailable": 100,
  "updatedAt": "2026-06-17T03:33:43"
}
```

---

#### Add Stock (Admin only)

```http
POST /api/v1/inventory
Content-Type: application/json
Authorization: Bearer <admin-token>

{
  "productId": "ee04ec8d-a1a3-4941-9f78-6dd19dfdc381",
  "productName": "Wireless Headphones",
  "quantityToAdd": 100
}
```

**Required fields:** `productId`, `productName`, `quantityToAdd` (min 1)

**Response — 200 OK**
```json
{
  "id": "...",
  "productId": "ee04ec8d-a1a3-4941-9f78-6dd19dfdc381",
  "productName": "Wireless Headphones",
  "quantityAvailable": 100
}
```

---

### Orders

#### Place Order (authenticated)

```http
POST /api/v1/orders
Content-Type: application/json
Authorization: Bearer <token>

{
  "items": [
    {
      "productId": "ee04ec8d-a1a3-4941-9f78-6dd19dfdc381",
      "productName": "Wireless Headphones",
      "quantity": 2,
      "unitPrice": 99.99
    }
  ]
}
```

**Required fields per item:** `productId`, `productName`, `quantity` (min 1), `unitPrice`

**Response — 201 Created**
```json
{
  "id": "f07d7d48-402c-41bf-a461-330141f5fb16",
  "userId": "f3964091-b47b-43d0-91db-916cb5bcb66c",
  "userEmail": "admin@shopflow.com",
  "status": "PENDING",
  "totalAmount": 199.98,
  "items": [
    {
      "productId": "ee04ec8d-a1a3-4941-9f78-6dd19dfdc381",
      "productName": "Wireless Headphones",
      "quantity": 2,
      "unitPrice": 99.99,
      "subtotal": 199.98
    }
  ],
  "createdAt": "2026-06-17T17:21:34.143123582",
  "updatedAt": "2026-06-17T17:21:34.143123582"
}
```

Order starts as `PENDING`. With Kafka running, it transitions to `CONFIRMED` after inventory is reserved, or `CANCELLED` if stock is insufficient.

---

#### List My Orders (authenticated)

```http
GET /api/v1/orders?page=0&size=10
Authorization: Bearer <token>
```

**Response — 200 OK** — paginated list of the authenticated user's orders

---

#### Get Order by ID (authenticated)

```http
GET /api/v1/orders/{id}
Authorization: Bearer <token>
```

**Response — 200 OK** — order object (only accessible by the order's owner)

---

## Order Status Lifecycle

```
PENDING  →  CONFIRMED   (inventory reserved via Kafka)
         →  CANCELLED   (insufficient stock via Kafka)
```

---

## Error Responses

All errors follow [RFC 9457 Problem Details](https://datatracker.ietf.org/doc/html/rfc9457):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/products",
  "errors": {
    "sku": "SKU is required",
    "price": "Price is required"
  }
}
```

| Status | Meaning |
|---|---|
| 400 | Validation failed — `errors` field lists field-level messages |
| 401 | Missing or invalid JWT token |
| 403 | Authenticated but insufficient role (e.g. USER trying admin endpoint) |
| 404 | Resource not found |
| 500 | Internal server error |

---

## Kafka Topics

| Topic | Producer | Consumer | Trigger |
|---|---|---|---|
| `order.created` | Order Service | Inventory Service | Order placed |
| `inventory.reserved` | Inventory Service | Order Service | Stock reserved |
| `inventory.failed` | Inventory Service | Order Service | Insufficient stock |
| `order.confirmed` | Order Service | Notification Service | Inventory confirmed |
| `order.cancelled` | Order Service | Notification Service | Order cancelled |

Kafka is optional — the API functions fully without it. Events are silently skipped when the broker is unavailable.

---

## Running Locally

**Prerequisites:** Java 17, Maven 3.9+, Docker Desktop

### 1. Start infrastructure

```bash
docker compose up -d
```

Starts: PostgreSQL, Redis, Kafka, Zookeeper, Kafka UI (http://localhost:8090)

### 2. Build and run the monolith

```bash
cd shopflow-monolith
mvn spring-boot:run
```

API available at: `http://localhost:8080`

### 3. Environment variables (optional overrides)

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/shopflow_db` | PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | `shopflow` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `shopflow123` | DB password |
| `JWT_SECRET` | dev default | Must be 256-bit min in production |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |

---

## Project Structure

```
shopflow/
├── docker-compose.yml
├── Dockerfile.monolith
├── shopflow-monolith/       ← deployable artifact
│   └── src/main/java/com/shopflow/monolith/
│       ├── config/          ← SecurityConfig, JwtAuthFilter, DataInitializer
│       └── controller/
├── user-service/
├── product-service/
├── order-service/
├── inventory-service/
├── notification-service/
├── api-gateway/             ← local dev only
└── eureka-server/           ← local dev only
```

Each service module follows:
```
src/main/java/com/shopflow/<service>/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── event/          ← Kafka event POJOs
├── config/
└── exception/
```
