# Ecomm microservices

A Spring Boot project with a separate React dashboard for viewing products,
orders, and data exchanged between services. The frontend lives in `frontend/`
and uses Vite. It has no authentication.

## Service flow

1. The browser creates products and orders through the gateway.
2. Order service fetches the product over HTTP with OpenFeign, checks available
   stock, saves an order as `PENDING_PAYMENT`, and publishes `order-created`.
3. Payment service consumes that event, saves a payment, and publishes
   `payment-completed` or `payment-failed`.
4. Order service consumes the payment result and changes the order to `CONFIRMED`
   or `PAYMENT_FAILED`. Notification service consumes all three event types and
   logs simulated notifications.

Eureka provides service discovery; Config Server supplies shared/service settings.
Products, orders, and payments use separate PostgreSQL databases.

## Run locally

Prerequisites: Java 21, PostgreSQL on `localhost:5432`, and Kafka on
`localhost:9092`. Maven is supplied by each service's `mvnw` wrapper. The default
database credentials in `ecomm-config-repo` are `postgres` / `postgres`.
Create the three databases if they do not already exist:

```sql
CREATE DATABASE ecomm_product_db;
CREATE DATABASE ecomm_order_db;
CREATE DATABASE ecomm_payment_db;
```

Kafka needs the topics `order-created`, `payment-completed`, and `payment-failed`
(or broker topic auto-creation enabled). Set `KAFKA_BOOTSTRAP_SERVERS` on the
consuming/publishing services if your Kafka address differs.

From the repository root, start Config Server in one terminal. It reads the local
`ecomm-config-repo/` working tree by default:

```bash
cd config-server
./mvnw spring-boot:run
```

Confirm `http://localhost:8888/product-service/default` returns JSON with
`product-service.yml` before starting Product Service. If using a Git-backed
config source, set `CONFIG_SERVER_PROFILE=git` and `CONFIG_REPO_URI` to its URL;
make the gateway and notification changes available in that Git repository.
Product Service can point to a different Config Server with
`CONFIG_SERVER_URL=http://host:port`.

Start discovery next, then each remaining service in its own terminal. Run
`./mvnw spring-boot:run` from each directory:

| Directory | Default port | Purpose |
| --- | --- | --- |
| `config-server` | 8888 | Shared configuration (start as above) |
| `discovery-service` | 8761 | Eureka registry |
| `product-service` | 8081 | Product catalog |
| `order-service` | 8082 | Order API and payment-result consumer |
| `payment-service` | 8083 configured | Kafka payment processor |
| `notification-service` | 8084 | Notification consumer and event feed |
| `api-gateway` | 8080 | Dashboard and API routing |

Start the React frontend in another terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173/**. Vite proxies `/api` requests to the gateway at
`http://localhost:8080`, so the browser can use the existing routes without a
cross-origin configuration. If the gateway is elsewhere, set
`VITE_GATEWAY_TARGET=http://host:port` before starting Vite. For a built frontend,
run `npm run build` and `npm run preview`; preview uses the same API proxy. When
hosting `dist/` separately, configure that host to proxy `/api/*` to the gateway.
The dashboard retries reads every three seconds while visible.

## Try the dashboard

1. Create a product with unit price `50` and stock `200`.
2. Place an order for quantity `100` with a demo email. The total is `5,000`, so
   the current payment implementation accepts it.
3. Select the order to see its status, response JSON, and matching Kafka events.
   Expand an event to inspect its payload, including IDs and timestamps.
4. Order quantity `101`. The total exceeds `5,000`, so payment fails; inspect the
   failure reason in the `payment-failed` payload.
5. Expand **Browser requests** to compare each submitted JSON body and response.
   Pause auto-refresh or use **Refresh now** as needed.

Existing business behavior: amounts have no currency field, stock is checked but
not decremented, and notifications are logged rather than emailed. The dashboard
does not change those behaviors.

## API additions and observation limits

| Endpoint | Service | Response |
| --- | --- | --- |
| `GET /api/products` | Product | Existing product list service method exposed over HTTP |
| `GET /api/events` | Notification, via gateway | Latest received Kafka observations, newest first |
| `GET /api/events?orderId=<uuid>` | Notification, via gateway | Retained observations for one order |

An observation contains `id`, `topic`, `source`, `consumer`, `orderId`,
`receivedAt`, and the consumed event `payload`. Order-created events retain their
original `eventId` and `occurredAt` as well.

The event feed holds **200 observations in memory per notification-service
instance** and resets on restart. Run one notification instance for a complete
local demonstration; replicas in the same Kafka consumer group receive different
messages and have separate histories. This is an inspection aid, not a durable
event archive or distributed tracing system. A received notification event does
not prove other consumers received it. Order status comes from the order API.
Messages may appear in a different receipt order across Kafka topics.

The browser keeps the latest 20 creation requests in memory for the current page
session. It does not record internal HTTP calls or Kafka headers. The frontend
calls `/api/*` through the Vite proxy in development and preview.

## Checks

Event-store and consumer tests, without requiring running infrastructure:

```bash
cd notification-service
./mvnw -Dtest=EventStoreTests,OrderCreatedConsumerTests test
```

Each service's `./mvnw test` also runs its existing Spring application-context test,
which expects its configured infrastructure to be available.
