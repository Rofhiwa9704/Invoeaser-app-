#!/bin/bash

echo "🚀 Starting Invoeaser Infrastructure Services..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Start services using Docker Compose
echo "📦 Starting PostgreSQL, Redis, and RabbitMQ..."
docker-compose up -d

# Wait a moment for services to start
echo "⏳ Waiting for services to initialize..."
sleep 10

# Check service health
echo "🔍 Checking service health..."

# Check PostgreSQL
if docker exec postgres-invoeaser pg_isready -U postgres > /dev/null 2>&1; then
    echo "✅ PostgreSQL is ready"
else
    echo "❌ PostgreSQL is not ready"
fi

# Check Redis
if docker exec redis-invoeaser redis-cli ping > /dev/null 2>&1; then
    echo "✅ Redis is ready"
else
    echo "❌ Redis is not ready"
fi

# Check RabbitMQ
if curl -s http://localhost:15672 > /dev/null 2>&1; then
    echo "✅ RabbitMQ Management UI is ready"
else
    echo "❌ RabbitMQ Management UI is not ready"
fi

echo ""
echo "🎉 Infrastructure services are starting up!"
echo ""
echo "📋 Service Information:"
echo "   🐘 PostgreSQL: localhost:5432 (db: invoeaser_db, user: postgres, pass: postgres)"
echo "   🔴 Redis: localhost:6379"
echo "   🐰 RabbitMQ: localhost:5672 (Management UI: http://localhost:15672, user: guest, pass: guest)"
echo ""
echo "🚀 You can now start the application with:"
echo "   ./mvnw spring-boot:run"
echo ""
echo "📖 View logs with:"
echo "   docker-compose logs -f"
echo ""
echo "🛑 Stop services with:"
echo "   docker-compose down"