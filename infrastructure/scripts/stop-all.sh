#!/bin/bash

# VelocityX - Stop All Services
# This script stops all microservices and infrastructure

set -e

echo "🛑 Stopping VelocityX Platform..."
echo ""

# Colors
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Change to docker directory
cd "$(dirname "$0")/../docker"

# Stop all services
echo -e "${BLUE}Stopping all microservices...${NC}"
docker-compose down

# Stop monitoring
echo -e "${BLUE}Stopping monitoring stack...${NC}"
cd ../monitoring
docker-compose -f docker-compose.monitoring.yml down

# Stop logging (if running)
echo -e "${BLUE}Stopping logging stack...${NC}"
cd ../logging
docker-compose -f docker-compose.logging.yml down 2>/dev/null || true

echo ""
echo -e "${RED}✅ VelocityX Platform stopped successfully!${NC}"
echo ""
echo "💾 Data volumes are preserved. To remove them:"
echo "  docker volume prune"
echo ""
