#!/bin/bash

# setup.sh - Complete setup script for the microservices architecture
# Compatible with: Linux, macOS, Windows (WSL/Git Bash)
# Prerequisites: Docker Desktop/colima, Java 17+

set -e  # Exit on any error

# Detect operating system
OS="unknown"
case "$(uname -s)" in
    Linux*)     OS=Linux;;
    Darwin*)    OS=Mac;;
    CYGWIN*|MINGW*|MSYS*) OS=Windows;;
esac

echo "🚀 Starting complete setup of microservices architecture..."
echo "============================================================="

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

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
print_status "Checking prerequisites on $OS..."

# Check Docker
if ! command_exists docker; then
    print_error "Docker is not installed or not in PATH"
    case $OS in
        Mac) print_error "Install Docker Desktop from: https://docs.docker.com/desktop/mac/install/" ;;
        Linux) print_error "Install Docker from: https://docs.docker.com/engine/install/" ;;
        Windows) print_error "Install Docker Desktop from: https://docs.docker.com/desktop/windows/install/" ;;
    esac
    exit 1
fi

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    print_error "Docker is installed but not running. Please start Docker Desktop/Docker daemon."
    exit 1
fi

# Check Java
if ! command_exists java; then
    print_error "Java is not installed or not in PATH"
    print_error "Install Java 17+ from: https://adoptium.net/"
    exit 1
fi

# Check Java version
java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$java_version" -lt 17 ] 2>/dev/null; then
    print_error "Java 17+ is required, found version: $java_version"
    exit 1
fi

# Check for port conflicts
print_status "Checking for port conflicts..."
ports_to_check="8081 8082 8083 8085 6379 9094 2181"
conflicted_ports=""

for port in $ports_to_check; do
    if command_exists netstat; then
        if netstat -an 2>/dev/null | grep -q ":$port "; then
            conflicted_ports="$conflicted_ports $port"
        fi
    elif command_exists ss; then
        if ss -an 2>/dev/null | grep -q ":$port "; then
            conflicted_ports="$conflicted_ports $port"
        fi
    fi
done

if [ -n "$conflicted_ports" ]; then
    print_warning "The following ports are already in use:$conflicted_ports"
    print_warning "Please stop services using these ports or they will conflict"
fi

print_success "Prerequisites check passed"

# Clean up any existing containers/networks
print_status "Cleaning up any existing containers and networks..."
docker stop zookeeper kafka redis order-service-app inventory-service-app notification-service-app kafka-ui 2>/dev/null || true
docker rm zookeeper kafka redis order-service-app inventory-service-app notification-service-app kafka-ui 2>/dev/null || true
docker network rm liron-network 2>/dev/null || true

print_success "Cleanup completed"

# Function to build a service
build_service() {
    local service_name=$1
    print_status "Building $service_name..."
    
    cd "$service_name"
    
    # Make mvnw executable (important for cross-platform compatibility)
    chmod +x mvnw
    
    # Use ./mvnw or fallback to mvn
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests -q
    elif command_exists mvn; then
        print_warning "mvnw not found, using system mvn"
        mvn clean package -DskipTests -q
    else
        print_error "Neither ./mvnw nor mvn found"
        exit 1
    fi
    
    if [ $? -ne 0 ]; then
        print_error "Failed to build $service_name"
        exit 1
    fi
    
    cd ..
    print_success "$service_name built successfully"
}

# Build Java applications
print_status "Building Java applications..."
build_service "order-service"
build_service "inventory-service"  
build_service "notification-service"

print_success "All Java applications built successfully"

# Build Docker images
print_status "Building Docker images..."

docker build -t order-service ./order-service
if [ $? -ne 0 ]; then
    print_error "Failed to build order-service Docker image"
    exit 1
fi

docker build -t inventory-service ./inventory-service
if [ $? -ne 0 ]; then
    print_error "Failed to build inventory-service Docker image"
    exit 1
fi

docker build -t notification-service ./notification-service
if [ $? -ne 0 ]; then
    print_error "Failed to build notification-service Docker image"
    exit 1
fi

print_success "All Docker images built successfully"

# Create custom network
print_status "Creating custom Docker network..."
docker network create liron-network
print_success "Network 'liron-network' created"

# Start infrastructure containers
print_status "Starting infrastructure containers..."

# Start Zookeeper
print_status "Starting Zookeeper..."
docker run -d --name zookeeper --network liron-network \
    -p 2181:2181 \
    -e ALLOW_ANONYMOUS_LOGIN=yes \
    bitnami/zookeeper:3.7

# Start Redis
print_status "Starting Redis..."
docker run -d --name redis --network liron-network \
    -p 6379:6379 \
    redis:7-alpine

# Start Kafka
print_status "Starting Kafka..."
docker run -d --name kafka --network liron-network \
    -p 9094:9094 \
    -e KAFKA_ENABLE_KRAFT=yes \
    -e KAFKA_CFG_NODE_ID=1 \
    -e KAFKA_CFG_PROCESS_ROLES=broker,controller \
    -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
    -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093,EXTERNAL://:9094 \
    -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,EXTERNAL://localhost:9094 \
    -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT \
    -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true \
    bitnami/kafka:3.7

# Start Kafka UI
print_status "Starting Kafka UI..."
docker run -d --name kafka-ui --network liron-network \
    -p 8085:8080 \
    -e KAFKA_CLUSTERS_0_NAME=local \
    -e KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS=kafka:9092 \
    provectuslabs/kafka-ui

print_success "Infrastructure containers started"

# Wait for infrastructure to be ready
print_status "Waiting for infrastructure to be ready..."

# Check if Kafka is ready (it usually starts within 10 seconds)
print_status "Checking Kafka readiness..."
max_attempts=15
attempt=1
while [ $attempt -le $max_attempts ]; do
    if docker logs kafka 2>&1 | grep -q "Kafka Server started"; then
        print_success "Kafka is ready"
        break
    fi
    if [ $attempt -eq $max_attempts ]; then
        print_warning "Kafka may not be fully ready, but continuing..."
        break
    fi
    
    # Show less verbose output for the first few attempts
    if [ $attempt -le 5 ]; then
        print_status "Waiting for Kafka to start... ($attempt/$max_attempts)"
    else
        print_status "Still waiting for Kafka... ($attempt/$max_attempts)"
    fi
    
    sleep 2
    ((attempt++))
done

# Start application containers
print_status "Starting application containers..."

# Start Order Service
print_status "Starting Order Service..."
docker run -d --name order-service-app --network liron-network \
    -p 8081:8080 \
    -e SPRING_PROFILES_ACTIVE=docker \
    -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    -e SPRING_REDIS_HOST=redis \
    order-service

# Start Inventory Service
print_status "Starting Inventory Service..."
docker run -d --name inventory-service-app --network liron-network \
    -p 8082:8080 \
    -e SPRING_PROFILES_ACTIVE=docker \
    -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    inventory-service

# Start Notification Service
print_status "Starting Notification Service..."
docker run -d --name notification-service-app --network liron-network \
    -p 8083:8080 \
    -e SPRING_PROFILES_ACTIVE=docker \
    -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    -e SPRING_REDIS_HOST=redis \
    notification-service

print_success "All application containers started"

# Wait for applications to be ready
print_status "Waiting for applications to start up..."
sleep 20

# Final status check
print_status "Checking final container status..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "============================================================="
print_success "🎉 Setup completed successfully!"
echo "============================================================="
echo ""
echo "📋 Service URLs:"
echo "   • Order Service:        http://localhost:8081"
echo "   • Inventory Service:    http://localhost:8082"
echo "   • Notification Service: http://localhost:8083"
echo "   • Kafka UI:            http://localhost:8085"
echo ""
echo "🔧 Infrastructure:"
echo "   • Kafka:               localhost:9094"
echo "   • Redis:               localhost:6379"
echo "   • Zookeeper:           localhost:2181"
echo ""
echo "📊 Useful commands:"
echo "   • View all containers:  docker ps"
echo "   • View logs:           docker logs <container-name>"
echo "   • Stop all:            docker stop \$(docker ps -q)"
echo "   • Remove all:          docker rm \$(docker ps -aq)"
echo ""
echo "🚀 Your microservices architecture is ready to use!"