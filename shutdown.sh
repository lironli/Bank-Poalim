#!/bin/bash

# shutdown.sh - Graceful shutdown script for the microservices architecture  
# Compatible with: Linux, macOS, Windows (WSL/Git Bash)
# This script stops and removes all containers and networks created by setup.sh

set -e  # Exit on any error

# Detect operating system
OS="unknown"
case "$(uname -s)" in
    Linux*)     OS=Linux;;
    Darwin*)    OS=Mac;;
    CYGWIN*|MINGW*|MSYS*) OS=Windows;;
esac

echo "🛑 Starting graceful shutdown of microservices architecture..."
echo "=============================================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if container exists and is running
container_exists() {
    docker ps -a --format '{{.Names}}' | grep -q "^$1$"
}

container_is_running() {
    docker ps --format '{{.Names}}' | grep -q "^$1$"
}

# Function to gracefully stop a container
stop_container() {
    local container_name=$1
    local service_name=$2
    
    if container_exists "$container_name"; then
        if container_is_running "$container_name"; then
            print_status "Stopping $service_name..."
            docker stop "$container_name" >/dev/null 2>&1
            print_success "$service_name stopped"
        else
            print_warning "$service_name was not running"
        fi
        
        print_status "Removing $service_name container..."
        docker rm "$container_name" >/dev/null 2>&1
        print_success "$service_name container removed"
    else
        print_warning "$service_name container not found"
    fi
}

# Function to check if network exists
network_exists() {
    docker network ls --format '{{.Name}}' | grep -q "^$1$"
}

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop/Docker daemon."
    exit 1
fi

# Show current running containers
print_status "Current running containers on $OS:"
if docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(order-service|inventory-service|notification-service|kafka|zookeeper|redis|kafka-ui)" >/dev/null 2>&1; then
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(order-service|inventory-service|notification-service|kafka|zookeeper|redis|kafka-ui)"
else
    print_warning "No project containers found running"
fi
echo ""

# Stop application containers first (graceful shutdown order)
print_status "Stopping application services..."
stop_container "order-service-app" "Order Service"
stop_container "inventory-service-app" "Inventory Service" 
stop_container "notification-service-app" "Notification Service"

print_success "All application services stopped"
echo ""

# Stop monitoring services
print_status "Stopping monitoring services..."
stop_container "kafka-ui" "Kafka UI"

print_success "Monitoring services stopped"
echo ""

# Stop infrastructure services (in reverse dependency order)
print_status "Stopping infrastructure services..."
stop_container "kafka" "Kafka"
stop_container "redis" "Redis"
stop_container "zookeeper" "Zookeeper"

print_success "All infrastructure services stopped"
echo ""

# Remove custom network
print_status "Cleaning up network..."
if network_exists "liron-network"; then
    print_status "Removing custom network 'liron-network'..."
    docker network rm liron-network >/dev/null 2>&1
    print_success "Network 'liron-network' removed"
else
    print_warning "Network 'liron-network' not found"
fi

# Clean up any orphaned containers (just in case)
print_status "Cleaning up any orphaned containers..."
orphaned_containers=$(docker ps -aq --filter "status=exited" | head -10)
if [ -n "$orphaned_containers" ]; then
    docker rm $orphaned_containers >/dev/null 2>&1 || true
    print_success "Orphaned containers cleaned up"
else
    print_status "No orphaned containers found"
fi

# Optional: Clean up images (commented out by default)
# Uncomment the following section if you want to remove the built images as well
# print_status "Cleaning up Docker images..."
# docker rmi order-service inventory-service notification-service >/dev/null 2>&1 || true
# print_success "Custom images removed"

# Show final status
print_status "Final container status check..."
remaining_containers=$(docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "(order-service|inventory-service|notification-service|kafka|zookeeper|redis|kafka-ui)" || true)

if [ -z "$remaining_containers" ]; then
    print_success "All project containers have been stopped and removed"
else
    print_warning "Some containers may still be running:"
    echo "$remaining_containers"
fi

# Show disk space freed (optional)
print_status "Cleaning up unused Docker resources..."
docker system prune -f >/dev/null 2>&1 || true
print_success "Docker system cleanup completed"

echo ""
echo "=============================================================="
print_success "🎉 Graceful shutdown completed!"
echo "=============================================================="
echo ""
echo "📊 Summary:"
echo "   ✅ Application services stopped and removed"
echo "   ✅ Infrastructure services stopped and removed"  
echo "   ✅ Custom network removed"
echo "   ✅ System cleanup performed"
echo ""
echo "🔧 Useful commands:"
echo "   • Check remaining containers: docker ps -a"
echo "   • Check remaining networks:   docker network ls"
echo "   • Check disk usage:          docker system df"
echo "   • Full system cleanup:       docker system prune -a"
echo ""
echo "🚀 To restart the system, run: ./setup.sh"