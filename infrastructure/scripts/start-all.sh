#!/bin/bash

# VelocityX - Start All Services
# This script starts all microservices and infrastructure

set -e

echo "🚀 Starting VelocityX Platform..."
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Change to docker directory
cd "$(dirname "$0")/../docker"

# Create network if it doesn't exist
echo -e "${BLUE}Creating Docker network...${NC}"
docker network create velocityx-network 2>/dev/null || echo "Network already exists"

# Start infrastructure first
echo -e "${BLUE}Starting infrastructure services...${NC}"
docker-compose up -d postgres redis zookeeper kafka

# Wait for infrastructure to be ready
echo -e "${YELLOW}Waiting for infrastructure to be ready...${NC}"
sleep 20

# Start Eureka Server
echo -e "${BLUE}Starting Eureka Server...${NC}"
docker-compose up -d eureka-server

# Wait for Eureka
echo -e "${YELLOW}Waiting for Eureka Server...${NC}"
sleep 15

# Start all microservices
echo -e "${BLUE}Starting microservices...${NC}"
docker-compose up -d api-gateway user-service transaction-service wallet-service reward-service notification-service

# Wait for services to start
echo -e "${YELLOW}Waiting for services to start...${NC}"
sleep 30

# Start monitoring (optional)
echo -e "${BLUE}Starting monitoring stack...${NC}"
cd ../monitoring
docker-compose -f docker-compose.monitoring.yml up -d

echo ""
echo -e "${GREEN}✅ VelocityX Platform started successfully!${NC}"
echo ""
echo "📊 Access Points:"
echo "  - API Gateway:    http://localhost:8080"
echo "  - Eureka Server:  http://localhost:8761"
echo "  - Prometheus:     http://localhost:9090"
echo "  - Grafana:        http://localhost:3000 (admin/admin)"
echo "  - Kafka UI:       http://localhost:8090"
echo ""
echo "🔍 Check service health:"
echo "  docker-compose ps"
echo ""
echo "📝 View logs:"
echo "  docker-compose logs -f [service-name]"
echo ""
