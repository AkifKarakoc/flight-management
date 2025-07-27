#!/bin/bash

set -e

echo "🚀 Flight Management System - Docker Build & Run"
echo "=================================================="

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p docker/mysql/init
mkdir -p logs

# Build JAR files
echo "🔨 Building Reference Manager Service..."
cd reference-manager-service
./mvnw clean package -DskipTests
cd ..

echo "🔨 Building Flight Service..."
cd flight-service
./mvnw clean package -DskipTests
cd ..

# Stop existing containers if running
echo "🛑 Stopping existing containers..."
docker-compose down

# Remove old images (optional)
echo "🗑️ Removing old images..."
docker-compose down --rmi local || true

# Build and start all services
echo "🐳 Building and starting Docker containers..."
docker-compose up --build -d

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 30

# Check service health
echo "🔍 Checking service health..."
echo "MySQL: $(docker-compose exec mysql mysqladmin ping -h localhost -u root -p123456 2>/dev/null && echo "✅ UP" || echo "❌ DOWN")"
echo "Redis: $(docker-compose exec redis redis-cli ping 2>/dev/null && echo "✅ UP" || echo "❌ DOWN")"
echo "Kafka: $(docker-compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:29092 >/dev/null 2>&1 && echo "✅ UP" || echo "❌ DOWN")"

# Wait a bit more for Spring Boot apps
echo "⏳ Waiting for Spring Boot applications..."
sleep 60

echo "🌐 Checking application health..."
echo "Reference Manager: $(curl -s http://localhost:8081/reference-manager/actuator/health | grep -q "UP" && echo "✅ UP" || echo "❌ DOWN")"
echo "Flight Service: $(curl -s http://localhost:8082/flight-service/actuator/health | grep -q "UP" && echo "✅ UP" || echo "❌ DOWN")"

echo ""
echo "🎉 Flight Management System is ready!"
echo "=================================================="
echo "📋 Service URLs:"
echo "   • Reference Manager: http://localhost:8081/reference-manager"
echo "   • Flight Service: http://localhost:8082/flight-service"
echo "   • Kafka UI: http://localhost:8080"
echo "   • MySQL: localhost:3306 (root/123456)"
echo "   • Redis: localhost:6379"
echo ""
echo "📖 API Documentation:"
echo "   • Reference Manager Swagger: http://localhost:8081/reference-manager/swagger-ui.html"
echo ""
echo "🔧 Useful commands:"
echo "   • View logs: docker-compose logs -f [service-name]"
echo "   • Stop all: docker-compose down"
echo "   • Restart: docker-compose restart [service-name]"