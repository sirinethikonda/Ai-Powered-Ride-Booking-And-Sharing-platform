-- Enable PostGIS and UUID generation extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) UNIQUE NOT NULL,
    gender VARCHAR(30) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone_number);

-- 2. Drivers Table
CREATE TABLE drivers (
    id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    license_number VARCHAR(100) UNIQUE NOT NULL,
    operational_status VARCHAR(30) DEFAULT 'OFFLINE' NOT NULL,
    rating DECIMAL(3, 2) DEFAULT 5.00 NOT NULL,
    total_trips INT DEFAULT 0 NOT NULL,
    current_location geometry(Point, 4326),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_drivers_status ON drivers(operational_status);
CREATE INDEX idx_drivers_location ON drivers USING GIST(current_location);

-- 3. Vehicles Table
CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    license_plate VARCHAR(50) UNIQUE NOT NULL,
    color VARCHAR(50) NOT NULL,
    category VARCHAR(30) NOT NULL,
    seat_capacity INT NOT NULL DEFAULT 4,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_vehicles_driver_id ON vehicles(driver_id);

-- 4. Shared Ride Groups Table
CREATE TABLE shared_ride_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    gender_policy VARCHAR(50) NOT NULL DEFAULT 'STRICT_GENDER_COMPATIBLE',
    allowed_gender VARCHAR(30) NOT NULL,
    max_detour_percent DECIMAL(5, 2) DEFAULT 30.00 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_shared_groups_status ON shared_ride_groups(status);

-- 5. Rides Table
CREATE TABLE rides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    passenger_id UUID NOT NULL REFERENCES users(id),
    driver_id UUID REFERENCES drivers(id),
    shared_group_id UUID REFERENCES shared_ride_groups(id),
    ride_type VARCHAR(30) NOT NULL DEFAULT 'SOLO',
    status VARCHAR(40) NOT NULL DEFAULT 'REQUESTED',
    pickup_address VARCHAR(500) NOT NULL,
    pickup_geom geometry(Point, 4326) NOT NULL,
    dropoff_address VARCHAR(500) NOT NULL,
    dropoff_geom geometry(Point, 4326) NOT NULL,
    estimated_distance_km DECIMAL(8, 2) NOT NULL,
    estimated_duration_min INT NOT NULL,
    actual_distance_km DECIMAL(8, 2),
    actual_duration_min INT,
    cancellation_reason VARCHAR(255),
    cancelled_by VARCHAR(30),
    idempotency_key VARCHAR(100) UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_rides_passenger ON rides(passenger_id);
CREATE INDEX idx_rides_driver ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
CREATE INDEX idx_rides_pickup_geom ON rides USING GIST(pickup_geom);
CREATE INDEX idx_rides_idempotency ON rides(idempotency_key);

-- 6. Ride Assignments Table
CREATE TABLE ride_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ride_id UUID NOT NULL REFERENCES rides(id) ON DELETE CASCADE,
    driver_id UUID NOT NULL REFERENCES drivers(id),
    vehicle_id UUID REFERENCES vehicles(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    replacement_reason VARCHAR(255),
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    unassigned_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_assignments_ride ON ride_assignments(ride_id);
CREATE INDEX idx_assignments_driver ON ride_assignments(driver_id);
CREATE INDEX idx_assignments_status ON ride_assignments(status);

-- 7. Driver Offers Table
CREATE TABLE driver_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ride_id UUID NOT NULL REFERENCES rides(id) ON DELETE CASCADE,
    driver_id UUID NOT NULL REFERENCES drivers(id),
    status VARCHAR(30) NOT NULL DEFAULT 'OFFERED',
    attempt_number INT NOT NULL DEFAULT 1,
    estimated_earnings DECIMAL(10, 2) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_driver_offers_ride ON driver_offers(ride_id);
CREATE INDEX idx_driver_offers_driver ON driver_offers(driver_id);
CREATE INDEX idx_driver_offers_status_expiry ON driver_offers(status, expires_at);

-- 8. Ride Passengers (for shared rides) Table
CREATE TABLE ride_passengers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shared_group_id UUID NOT NULL REFERENCES shared_ride_groups(id) ON DELETE CASCADE,
    ride_id UUID NOT NULL REFERENCES rides(id) ON DELETE CASCADE,
    passenger_id UUID NOT NULL REFERENCES users(id),
    seats_reserved INT NOT NULL DEFAULT 1,
    pickup_sequence INT NOT NULL DEFAULT 1,
    dropoff_sequence INT NOT NULL DEFAULT 1,
    individual_fare DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_ride_passengers_group ON ride_passengers(shared_group_id);

-- 9. Fare Breakdowns Table
CREATE TABLE fare_breakdowns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ride_id UUID NOT NULL UNIQUE REFERENCES rides(id) ON DELETE CASCADE,
    base_fare DECIMAL(10, 2) NOT NULL,
    distance_fare DECIMAL(10, 2) NOT NULL,
    time_fare DECIMAL(10, 2) NOT NULL,
    surge_multiplier DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
    shared_discount_applied DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    final_fare DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_fare_breakdowns_ride ON fare_breakdowns(ride_id);

-- 10. Payments Table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ride_id UUID NOT NULL REFERENCES rides(id),
    passenger_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_payments_ride ON payments(ride_id);
CREATE INDEX idx_payments_status ON payments(status);

-- 11. Payment Transactions Table
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    gateway_reference VARCHAR(150),
    transaction_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_code VARCHAR(100),
    error_message VARCHAR(500),
    raw_response TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_payment_tx_payment_id ON payment_transactions(payment_id);

-- 12. Saga Instances Table
CREATE TABLE saga_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_type VARCHAR(50) NOT NULL,
    related_entity_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'STARTED',
    current_step VARCHAR(100),
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_saga_entity ON saga_instances(related_entity_id);
CREATE INDEX idx_saga_status ON saga_instances(status);

-- 13. Saga Steps Table
CREATE TABLE saga_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_instance_id UUID NOT NULL REFERENCES saga_instances(id) ON DELETE CASCADE,
    step_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_details TEXT,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_saga_steps_instance ON saga_steps(saga_instance_id);

-- 14. Transactional Outbox Events Table
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    partition_key VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);

-- 15. Processed Messages (Consumer Deduplication Table)
CREATE TABLE processed_messages (
    message_id VARCHAR(100) PRIMARY KEY,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);
