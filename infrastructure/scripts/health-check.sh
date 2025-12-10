#!/bin/bash

# VelocityX - Health Check Script
# This script checks the health of all services

echo "🏥 VelocityX Platform Health Check"
echo "===================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check service health
check_service() {
    local service_name=$1
    local url=$2
    
    if curl -f -s -o /dev/null "$url"; then
        echo -e "${GREEN}✓${NC} $service_name is healthy"
        return 0
    else
        echo -e "${RED}✗${NC} $service_name is unhealthy"
        return 1
    fi
}

# Infrastructure
echo "Infrastructure Services:"
check_service "PostgreSQL" "http://localhost:5432" || echo -e "${YELLOW}  (Check manually: psql -h localhost -U postgres)${NC}"
check_service "Redis" "http://localhost:6379" || echo -e "${YELLOW}  (Check manually: redis-cli ping)${NC}"
check_service "Kafka" "http://localhost:9092" || echo -e "${YELLOW}  (Check manually: kafka-topics.sh --list)${NC}"
echo ""

# Service Discovery
echo "Service Discovery:"
check_service "Eureka Server" "http://localhost:8761/actuator/health"
echo ""

# Microservices
echo "Microservices:"
check_service "API Gateway" "http://localhost:8080/actuator/health"
check_service "User Service" "http://localhost:8081/actuator/health"
check_service "Wallet Service" "http://localhost:8082/actuator/health"
check_service "Transaction Service" "http://localhost:8083/actuator/health"
check_service "Reward Service" "http://localhost:8084/actuator/health"
check_service "Notification Service" "http://localhost:8085/actuator/health"
echo ""

# Monitoring
echo "Monitoring:"
check_service "Prometheus" "http://localhost:9090/-/healthy"
check_service "Grafana" "http://localhost:3000/api/health"
echo ""

# Summary
echo "===================================="
echo "Health check complete!"
echo ""
echo "📊 View detailed metrics: http://localhost:9090"
echo "📈 View dashboards: http://localhost:3000"
echo ""
