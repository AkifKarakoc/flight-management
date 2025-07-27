# Flight Service

Operational flight management service with CSV upload capabilities, real-time updates, and reference data integration.

## Features

- **Operational Flight Management**: CRUD operations for flight data
- **CSV Upload**: Bulk flight data import with conflict detection
- **Real-time Updates**: WebSocket notifications for flight status changes
- **Reference Data Integration**: Automatic enrichment from Reference Manager Service
- **Conflict Detection**: Aircraft double booking, flight number duplicates, slot conflicts
- **Version Control**: Flight change tracking and audit trail
- **Caching**: Redis-based caching for reference data
- **Event Publishing**: Kafka events for flight lifecycle

## Prerequisites

- Java 21
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Kafka 3.0+
- Reference Manager Service (running on port 8081)

## Quick Start

### 1. Start Infrastructure Services

```bash
# Start MySQL (if not running)
# Start Redis (if not running)
# Start Kafka (if not running)
```

### 2. Start Reference Manager Service

```bash
cd ../reference-manager-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Start Flight Service

```bash
# From flight-service directory
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Or with environment variables:

```bash
export DB_USERNAME=root
export DB_PASSWORD=123456
export REDIS_HOST=localhost
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export REFERENCE_MANAGER_URL=http://localhost:8081/reference-manager

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## API Endpoints

### Health Check
- `GET /flight-service/actuator/health`

### Flight Operations
- `GET /flight-service/api/v1/flights` - List flights
- `POST /flight-service/api/v1/flights` - Create flight
- `GET /flight-service/api/v1/flights/{id}` - Get flight by ID
- `PUT /flight-service/api/v1/flights/{id}` - Update flight
- `DELETE /flight-service/api/v1/flights/{id}` - Delete flight

### CSV Upload
- `POST /flight-service/api/v1/flights/upload` - Upload CSV file
- `GET /flight-service/api/v1/flights/uploads` - List upload batches

### Flight Status
- `GET /flight-service/api/v1/flights/status/dashboard` - Dashboard overview
- `GET /flight-service/api/v1/flights/live` - Live flight status

### WebSocket
- `ws://localhost:8082/flight-service/ws` - WebSocket endpoint

## Configuration

### Database
- **URL**: `jdbc:mysql://localhost:3306/flight_service_db`
- **Username**: `root` (configurable via `DB_USERNAME`)
- **Password**: `123456` (configurable via `DB_PASSWORD`)

### Redis
- **Host**: `localhost` (configurable via `REDIS_HOST`)
- **Port**: `6379` (configurable via `REDIS_PORT`)

### Kafka
- **Bootstrap Servers**: `localhost:9092` (configurable via `KAFKA_BOOTSTRAP_SERVERS`)

### Reference Manager
- **Base URL**: `http://localhost:8081/reference-manager` (configurable via `REFERENCE_MANAGER_URL`)

## Development

### Running Tests
```bash
./mvnw test
```

### Code Coverage
```bash
./mvnw jacoco:report
```

### Building
```bash
./mvnw clean package
```

## Architecture

- **Port**: 8082
- **Context Path**: `/flight-service`
- **Database**: MySQL with Liquibase migrations
- **Cache**: Redis with TTL-based expiration
- **Message Broker**: Kafka for event publishing
- **Real-time**: WebSocket with STOMP
- **Security**: JWT-based authentication
- **Circuit Breaker**: Resilience4j for external service calls

## Integration

This service integrates with:
- **Reference Manager Service**: For airline, station, and aircraft data
- **Flight Archive Service**: For data archiving (via Kafka events)
- **Notification Service**: For real-time notifications (via Kafka events)