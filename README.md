# AI-Powered Ride Booking Platform

A maintainable, production-ready backend for an **AI-Powered Ride Booking Platform** featuring **Intelligent Driver Replacement**, **Gender-Compatible Shared Rides**, **Conditional Shared Pricing**, **Saga Orchestration**, **Apache Kafka Event-Driven Architecture**, and **Resilient Payments with Resilience4j**.

---

## 1. System Architecture

Built as a **Modular Monolith** using **Java 21** and **Spring Boot 3.3.x**:

```
                              ┌──────────────────────────────────────────────┐
                              │           Spring Boot Application            │
                              │             (Modular Monolith)               │
                              │                                              │
                              │  ┌──────────────┐      ┌──────────────────┐  │
                              │  │ Auth & Users │      │ Driver & Location│  │
                              │  └──────┬───────┘      └────────┬─────────┘  │
                              │         │                       │            │
                              │  ┌──────▼───────┐      ┌────────▼─────────┐  │
                              │  │ Ride Booking │◄────►│ Dispatch & Repl. │  │
                              │  └──────┬───────┘      └────────┬─────────┘  │
                              │         │                       │            │
                              │  ┌──────▼───────┐      ┌────────▼─────────┐  │
                              │  │ Shared Rides │◄────►│  Saga Coordinator│  │
                              │  └──────────────┘      └────────┬─────────┘  │
                              │                                 │            │
                              │  ┌──────────────┐      ┌────────▼─────────┐  │
                              │  │ Resilience4j │◄────►│Payment & Pricing │  │
                              │  └──────────────┘      └────────┬─────────┘  │
                              │                                 │            │
                              │  ┌──────────────────────────────▼──────────┐ │
                              │  │ Transactional Outbox & Kafka Publisher  │ │
                              │  └──────────────────────┬──────────────────┘ │
                              └─────────────────────────┼────────────────────┘
                                                        │
                      ┌─────────────────────────────────┼──────────────────────────────┐
                      │                                 │                              │
                      ▼                                 ▼                              ▼
             ┌─────────────────┐               ┌─────────────────┐            ┌─────────────────┐
             │ PostgreSQL 16   │               │ Redis 7         │            │ Apache Kafka    │
             │ + PostGIS       │               │ Geospatial Cache│            │ (KRaft Mode)    │
             │ (System of Rec.)│               │ & Active Offers │            │ Event Streaming │
             └─────────────────┘               └─────────────────┘            └─────────────────┘
```

---

## 2. Technology Stack

- **Runtime & Language**: Java 21 (Virtual Threads / Project Loom enabled)
- **Framework**: Spring Boot 3.3.4
- **Database**: PostgreSQL 16 with PostGIS extension (`postgis/postgis:16-3.4`)
- **Database Migrations**: Flyway (15 normalized tables, spatial indexes, UUID keys)
- **Real-Time Geospatial Cache**: Redis 7 (`GEOADD`, `GEOSEARCH`, `GEORADIUS`)
- **Event Streaming**: **Apache Kafka 3.7.0** (KRaft mode without Zookeeper)
- **Reliable Messaging**: Transactional Outbox Pattern + Idempotent Kafka Consumers (`processed_messages`)
- **Distributed Workflow Coordination**: Persisted Saga Orchestrator (`saga_instances`, `saga_steps`)
- **Fault Tolerance**: Resilience4j (Circuit Breakers, Retries, Rate Limiters, Time Limiters)
- **Security**: Spring Security 6 + JWT (HMAC-SHA256, stateless sessions)
- **API Documentation**: Springdoc OpenAPI 2.6.0 / Swagger UI
- **Containerization**: Docker & Docker Compose

---

## 3. Core Business Features & Principles

### A. Idempotent Ride Creation
- Rides accept an optional `Idempotency-Key` header.
- Duplicate booking requests with the same key safely return the existing ride record without creating duplicate trips or double charges.

### B. Explicit Ride State Machine
```
REQUESTED ──► SEARCHING_FOR_DRIVER ──► DRIVER_ASSIGNED ──► DRIVER_ARRIVING ──► DRIVER_ARRIVED ──► IN_PROGRESS ──► COMPLETED
     │                    │                     │                     │                     │
     ▼                    ▼                     ▼                     ▼                     ▼
 CANCELLED            CANCELLED             CANCELLED             CANCELLED             CANCELLED
```
- Strict transition validation: Terminal states (`COMPLETED`, `CANCELLED`, `FAILED`) cannot transition.
- Optimistic locking via `@Version` prevents race conditions.

### C. Intelligent Driver Replacement
- Driver offers have a configurable TTL deadline (default: 30 seconds).
- If a driver **rejects**, **cancels**, or **times out**:
  1. The previous offer is marked `REJECTED` or `EXPIRED`.
  2. The candidate is excluded from future offers for this ride.
  3. The dispatch engine searches the next best ranked eligible driver within the radius.
  4. Automatic replacement is bounded (max 3 attempts). If exhausted, the ride terminates gracefully in `FAILED` status.

### D. Gender-Compatible Shared Rides
- Binary gender matching eligibility:
  - **Male + Male**: Eligible.
  - **Female + Female**: Eligible.
  - **Male + Female**: **Strictly Ineligible** (Never matched).
- Candidate rides must also satisfy pickup distance (<= 2.5 km) and dropoff distance (<= 3.5 km) corridor constraints.

### E. Conditional Shared Pricing
- Disclosed upfront:
  - **Solo Fare**: e.g., ₹200.
  - **Shared Fare**: e.g., ₹140 (30% discount).
- **Conditional Rule**: The 30% discount is applied **only if** a compatible match is confirmed.
- **Fallback Rule**: If no compatible match is found before departure, the passenger pays the disclosed solo fare (₹200) rather than receiving an unearned discount.

### F. Resilient Payment Gateway & Circuit Breakers
- Mock Payment Gateway supports deterministic simulation modes for testing:
  - `SUCCESS`: Normal transaction settlement.
  - `DECLINED`: Insufficient funds / card declined.
  - `NETWORK_ERROR`: Simulates upstream socket timeout (triggering Resilience4j Retry & Circuit Breaker).
  - `TIMEOUT`: Exceeds TimeLimiter duration.
  - `UNCERTAIN`: Retains `RECONCILIATION_REQUIRED` status to prevent duplicate charges.

---

## 4. Quick Start: Local Development

### Prerequisites
- Java 21 JDK
- Apache Maven 3.9+
- Docker & Docker Compose

### Step 1: Start Infrastructure Containers
Start PostgreSQL (with PostGIS), Redis, and Apache Kafka (KRaft mode):

```bash
docker compose up -d postgres redis kafka
```

Verify services are healthy:
```bash
docker compose ps
```

### Step 2: Build and Run Application
Run database migrations and start the Spring Boot application:

```bash
mvn clean spring-boot:run
```

The application starts on port `8080`.

### Step 3: Access Interactive Swagger UI
Open your browser and navigate to:
👉 **http://localhost:8080/swagger-ui.html**

---

## 5. API Walkthrough & Curl Examples

### 1. Register a Passenger
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "priya@example.com",
    "password": "Password123!",
    "fullName": "Priya Sharma",
    "phoneNumber": "+919876543210",
    "gender": "FEMALE",
    "role": "PASSENGER"
  }'
```

### 2. Register a Driver
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "driver1@example.com",
    "password": "Password123!",
    "fullName": "Kavitha Driver",
    "phoneNumber": "+919876543211",
    "gender": "FEMALE",
    "role": "DRIVER",
    "driverLicenseNumber": "DL-KA-2026-00123"
  }'
```

### 3. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "driver1@example.com",
    "password": "Password123!"
  }'
```
*Copy the returned `token` from the response.*

### 4. Register Vehicle & Set Driver Available
```bash
# Register vehicle
curl -X POST http://localhost:8080/api/v1/drivers/me/vehicle \
  -H "Authorization: Bearer <DRIVER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "make": "Hyundai",
    "model": "i20",
    "licensePlate": "KA-01-AB-1234",
    "color": "Silver",
    "category": "HATCHBACK",
    "seatCapacity": 4
  }'

# Update location in Redis GEO & PostGIS
curl -X PUT http://localhost:8080/api/v1/drivers/me/location \
  -H "Authorization: Bearer <DRIVER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 12.9716,
    "longitude": 77.5946,
    "heading": 90.0,
    "speedKmh": 25.0
  }'

# Mark status as AVAILABLE
curl -X PUT http://localhost:8080/api/v1/drivers/me/availability \
  -H "Authorization: Bearer <DRIVER_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "AVAILABLE"
  }'
```

### 5. Request a Fare Quote
```bash
curl -X POST http://localhost:8080/api/v1/ride-quotes \
  -H "Content-Type: application/json" \
  -d '{
    "pickupAddress": "MG Road, Bangalore",
    "pickupLatitude": 12.9756,
    "pickupLongitude": 77.6066,
    "dropoffAddress": "Koramangala 4th Block, Bangalore",
    "dropoffLatitude": 12.9345,
    "dropoffLongitude": 77.6265,
    "rideType": "SHARED"
  }'
```

### 6. Book Ride Idempotently
```bash
curl -X POST http://localhost:8080/api/v1/rides \
  -H "Authorization: Bearer <PASSENGER_JWT>" \
  -H "Idempotency-Key: BOOKING-REQ-UUID-001" \
  -H "Content-Type: application/json" \
  -d '{
    "pickupAddress": "MG Road, Bangalore",
    "pickupLatitude": 12.9756,
    "pickupLongitude": 77.6066,
    "dropoffAddress": "Koramangala 4th Block, Bangalore",
    "dropoffLatitude": 12.9345,
    "dropoffLongitude": 77.6265,
    "rideType": "SHARED",
    "maxDetourPercentage": 30.0
  }'
```

### 7. Configure Deterministic Payment Mock Mode
```bash
# Options: SUCCESS, DECLINED, NETWORK_ERROR, TIMEOUT, UNCERTAIN
curl -X POST "http://localhost:8080/api/v1/payments/mock-mode?mode=SUCCESS"
```

---

## 6. Running Automated Tests

Run the complete test suite:
```bash
mvn clean test
```

Test coverage includes:
- **Ride State Machine Transitions**: Forward progression and invalid state transition rejection.
- **Gender Compatibility**: M+M and F+F matching verification, M+F strict rejection.
- **Fare & Pricing Engine**: Haversine distance, solo fare, and 30% conditional shared discounts.
- **Intelligent Dispatch & Replacement**: Driver acceptance, rejection, TTL expiration, and 3-attempt limit.
- **Resilience4j Circuit Breaker & Fallback**: Mock payment states (SUCCESS, DECLINED, NETWORK_ERROR, UNCERTAIN reconciliation).

---

## 7. Kafka Topics Specification

| Topic | Partition Key | Purpose |
|---|---|---|
| `ride.events` | `rideId` | Publishes `RideRequestedEvent`, `RideCancelledEvent`, `TripStartedEvent`, `TripCompletedEvent` |
| `driver.offers` | `driverId` | Publishes `DriverOfferCreatedEvent` with TTL deadline |
| `driver.replacements` | `rideId` | Publishes `DriverReplacementRequestedEvent` when replacement is initiated |
| `shared-ride.events` | `sharedGroupId` | Publishes `SharedRideMatchConfirmedEvent` when two compatible passengers are matched |
| `payment.events` | `paymentId` | Publishes `PaymentRequestedEvent`, `PaymentSucceededEvent`, `PaymentFailedEvent` |

---

## 8. Full Docker Deployment

To build and run the entire multi-container environment (including backend app in Docker):

```bash
docker compose up -d --build
```
