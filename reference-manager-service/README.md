# Reference Manager Service

Reference data management service for the Flight Management System.

## Features

- User authentication and authorization with JWT
- Airline, Station, Aircraft, and Flight management
- Role-based access control (ADMIN, AIRLINE_USER)
- Redis caching for performance
- Kafka event publishing for data changes
- RESTful API with validation

## Quick Start

### Prerequisites

- JDK 21
- MySQL 8.0
- Redis
- Kafka

### Running the Service

1. **Database Setup:**
```bash
mysql -u root -p
CREATE DATABASE reference_manager_db;

### Flights
- `GET /api/v1/flights` - List flights
- `GET /api/v1/flights/{id}` - Get flight by ID
- `POST /api/v1/flights` - Create flight (Admin/Airline User)
- `PUT /api/v1/flights/{id}` - Update flight
- `DELETE /api/v1/flights/{id}` - Delete flight

### Route Segments
- `GET /api/v1/flights/{flightId}/segments` - Get flight segments
- `POST /api/v1/flights/{flightId}/segments` - Create segment
- `PUT /api/v1/segments/{segmentId}` - Update segment
- `DELETE /api/v1/segments/{segmentId}` - Delete segment

### Admin
- `GET /api/v1/admin/health` - System health
- `POST /api/v1/admin/cache/clear` - Clear all caches
- `GET /api/v1/admin/cache/stats` - Cache statistics