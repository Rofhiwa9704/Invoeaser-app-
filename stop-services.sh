#!/bin/bash

echo "🛑 Stopping Invoeaser Infrastructure Services..."

# Stop services using Docker Compose
docker-compose down

echo "✅ Services stopped successfully!"
echo ""
echo "💡 To remove all data volumes (CAUTION: This will delete all data!):"
echo "   docker-compose down -v"
echo ""
echo "🔄 To restart services:"
echo "   ./start-services.sh"