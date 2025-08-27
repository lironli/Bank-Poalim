# 🚀 Setup & Shutdown Scripts

This directory contains automated scripts to manage your microservices architecture across different platforms.

## 📋 Prerequisites

### Required Software:
- **Docker Desktop** (or Docker Engine + Docker Compose on Linux)
- **Java 17+** (OpenJDK recommended)
- **Bash shell** (see platform-specific notes below)

## 🖥️ Platform Compatibility

### ✅ **macOS**
- **Shell**: Built-in Bash works perfectly
- **Docker**: Docker Desktop recommended
- **Java**: Install via [Homebrew](https://brew.sh/): `brew install openjdk@17`
- **Run**: `./setup.sh` and `./shutdown.sh`

### ✅ **Linux** 
- **Shell**: Native Bash support
- **Docker**: Docker Engine or Docker Desktop
- **Java**: Install via package manager: `sudo apt install openjdk-17-jdk` (Ubuntu/Debian)
- **Run**: `./setup.sh` and `./shutdown.sh`

### ✅ **Windows**
- **Shell Options** (choose one):
  - **WSL2** (recommended): Full Linux compatibility
  - **Git Bash**: Comes with Git for Windows
  - **PowerShell**: Use `bash setup.sh` 
- **Docker**: Docker Desktop with WSL2 backend
- **Java**: Download from [Eclipse Temurin](https://adoptium.net/)
- **Run**: 
  - WSL/Git Bash: `./setup.sh`
  - PowerShell: `bash setup.sh`

## 🛠️ Script Features

### **setup.sh**
- ✅ **Cross-platform detection** (Linux/Mac/Windows)
- ✅ **Automatic prerequisites check**
  - Docker installation & daemon status
  - Java version (17+ required)
  - Port conflict detection
- ✅ **Smart Maven handling**
  - Fixes `mvnw` permissions automatically
  - Falls back to system `mvn` if needed
- ✅ **Robust error handling**
  - Clear error messages with helpful links
  - Graceful failure with cleanup
- ✅ **Progress tracking** with colored output

### **shutdown.sh** 
- ✅ **Graceful shutdown order** (apps → monitoring → infrastructure)
- ✅ **Smart container detection** (handles missing containers)
- ✅ **Complete cleanup** (containers, networks, orphaned resources)
- ✅ **Cross-platform network cleanup**

## 🚀 Quick Start

```bash
# Make scripts executable (Linux/Mac)
chmod +x setup.sh shutdown.sh

# Start everything
./setup.sh

# Stop everything  
./shutdown.sh
```

## 🔧 Troubleshooting

### **Permission Denied (Windows)**
```bash
# If ./setup.sh fails, try:
bash setup.sh
```

### **Maven Wrapper Issues**
The script automatically handles `mvnw` permissions, but if you see errors:
```bash
chmod +x */mvnw  # Fix permissions for all services
```

### **Port Conflicts**
If ports are in use, the script will warn you. Stop conflicting services:
```bash
# Find what's using a port (example: 8081)
lsof -i :8081        # Mac/Linux
netstat -ano | findstr :8081  # Windows
```

### **Docker Not Running**
Make sure Docker Desktop is started:
- **Mac**: Docker icon in menu bar
- **Windows**: Docker Desktop in system tray  
- **Linux**: `sudo systemctl start docker`

## 📊 What Gets Created

### **Containers:**
- `zookeeper` (port 2181)
- `kafka` (port 9094)
- `redis` (port 6379)
- `order-service-app` (port 8081)
- `inventory-service-app` (port 8082)
- `notification-service-app` (port 8083)
- `kafka-ui` (port 8085)

### **Network:**
- `liron-network` (custom Docker network)

### **Docker Images:**
- `order-service:latest`
- `inventory-service:latest`
- `notification-service:latest`

## 🎯 Service URLs

After successful setup:
- **Order Service**: http://localhost:8081
- **Inventory Service**: http://localhost:8082
- **Notification Service**: http://localhost:8083
- **Kafka UI**: http://localhost:8085

## 🔄 Development Workflow

```bash
# Full restart
./shutdown.sh && ./setup.sh

# Check container status
docker ps

# View logs
docker logs order-service-app
docker logs kafka

# Connect to containers
docker exec -it kafka bash
docker exec -it redis redis-cli
```

## 📈 Performance Notes

- **First run**: Downloads Docker images (~500MB total)
- **Subsequent runs**: Uses cached images (much faster)
- **Build time**: ~2-3 minutes depending on system
- **Startup time**: ~30 seconds for all services

## 🆘 Support

If you encounter issues:
1. Check the **Prerequisites** section
2. Review **Platform-specific notes** 
3. Run with verbose output: `bash -x setup.sh`
4. Check Docker logs: `docker logs <container-name>`

The scripts are designed to work out-of-the-box on most systems with Docker and Java installed! 🎉