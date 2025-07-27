# Flight Service - Reference Manager Integration Test

## Prerequisites
- MySQL running on localhost:3306
- Redis running on localhost:6379
- Kafka running on localhost:9092

## Test Senaryoları

### 1. Authentication Test
```bash
# 1. Register user in Reference Manager
curl -X POST http://localhost:8081/reference-manager/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "role": "AIRLINE_USER"
  }'

# 2. Login to get JWT token
curl -X POST http://localhost:8081/reference-manager/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 2. CRUD Operations Test
```bash
# 1. Create flight (with JWT token)
curl -X POST http://localhost:8082/flight-service/api/v1/flights \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d '{
    "flightNumber": "TK123",
    "airlineId": 1,
    "aircraftId": 1,
    "originStationId": 1,
    "destinationStationId": 2,
    "scheduledDepartureTime": "2024-01-15T10:00:00",
    "scheduledArrivalTime": "2024-01-15T12:00:00",
    "flightType": "PASSENGER"
  }'

# 2. Get flight by ID
curl -X GET http://localhost:8082/flight-service/api/v1/flights/1 \
  -H "Authorization: Bearer {JWT_TOKEN}"

# 3. Update flight
curl -X PUT http://localhost:8082/flight-service/api/v1/flights/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d '{
    "flightNumber": "TK123",
    "airlineId": 1,
    "aircraftId": 1,
    "originStationId": 1,
    "destinationStationId": 2,
    "scheduledDepartureTime": "2024-01-15T11:00:00",
    "scheduledArrivalTime": "2024-01-15T13:00:00",
    "flightType": "PASSENGER"
  }'

# 4. Delete flight
curl -X DELETE http://localhost:8082/flight-service/api/v1/flights/1 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 3. CSV Upload Test
```bash
# 1. Create CSV file
echo "flightNumber,airlineId,aircraftId,originStationId,destinationStationId,scheduledDepartureTime,scheduledArrivalTime,flightType
TK123,1,1,1,2,2024-01-15T10:00:00,2024-01-15T12:00:00,PASSENGER
TK124,1,1,2,1,2024-01-15T14:00:00,2024-01-15T16:00:00,PASSENGER" > flights.csv

# 2. Upload CSV file
curl -X POST http://localhost:8082/flight-service/api/v1/flights/upload \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -F "file=@flights.csv"
```

### 4. Reference Data Integration Test
```bash
# 1. Check if reference data is accessible
curl -X GET http://localhost:8081/reference-manager/api/v1/airlines/1
curl -X GET http://localhost:8081/reference-manager/api/v1/stations/1
curl -X GET http://localhost:8081/reference-manager/api/v1/aircraft/1

# 2. Test flight service reference data fallback
# (Reference Manager down scenario)
```

## Expected Results

### ✅ Success Scenarios:
1. **Authentication**: JWT token alınabilmeli
2. **CRUD**: Flight oluşturma, okuma, güncelleme, silme çalışmalı
3. **CSV Upload**: Dosya yükleme ve işleme başarılı olmalı
4. **Reference Data**: Flight oluştururken reference data otomatik enrich edilmeli

### ⚠️ Fallback Scenarios:
1. **Reference Manager Down**: Flight service cache'den veri almalı
2. **Network Issues**: Circuit breaker devreye girmeli
3. **Invalid Data**: Validation hataları döndürmeli

## Health Checks
```bash
# Reference Manager Health
curl http://localhost:8081/reference-manager/actuator/health

# Flight Service Health
curl http://localhost:8082/flight-service/actuator/health
``` 