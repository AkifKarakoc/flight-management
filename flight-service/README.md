# Flight Service

Operational flight management service for the Flight Management System.

## Features

- Operational flight CRUD operations
- CSV bulk upload with conflict detection
- Real-time WebSocket updates
- Flight versioning system
- Kafka event publishing
- Redis caching integration

## Quick Start

### Prerequisites

- JDK 21
- MySQL 8.0
- Redis
- Kafka
- Reference Manager Service running

### Running the Service

1. **Database Setup:**
```bash
mysql -u root -p
CREATE DATABASE flight_service_db;

### Flight Status
- `PATCH /api/v1/flights/{id}/status` - Update flight status
- `GET /api/v1/flights/live` - Live flight status
- `GET /api/v1/flights/dashboard` - Dashboard overview

### Real-time Features
- WebSocket endpoint: `/ws`
- Flight updates: `/topic/flights/{id}`
- Upload progress: `/topic/uploads/{batchId}`
- Dashboard updates: `/topic/dashboard`

## CSV Upload Format

```csv
flightNumber,airlineCode,aircraftType,flightDate,scheduledDepartureTime,scheduledArrivalTime,originIcaoCode,destinationIcaoCode,flightType,gate,terminal
TK123,TK,A320,2025-01-15,10:30,13:45,LTBA,EDDF,PASSENGER,A12,1
LH456,LH,B737,2025-01-15,14:00,17:30,EDDF,KJFK,PASSENGER,B5,2