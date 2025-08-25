# Order Service

A Spring Boot microservice for processing orders in a distributed system.

## Features

- REST API for order creation
- Input validation
- Error handling
- Kafka event publishing for order events
- Comprehensive test coverage

## API Documentation

### POST /orders

Creates a new order.

#### Request

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "customerName": "Alice",
  "items": [
    {
      "productId": "P1001",
      "quantity": 2,
      "category": "standard"
    }
  ],
  "requestedAt": "2025-06-30T14:00:00Z"
}
```

#### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `customerName` | String | Yes | Name of the customer placing the order |
| `items` | Array | Yes | List of items in the order (must contain at least one item) |
| `requestedAt` | ISO 8601 DateTime | Yes | Timestamp when the order was requested |

#### Item Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `productId` | String | Yes | Unique identifier for the product |
| `quantity` | Integer | Yes | Quantity of the product (must be positive) |
| `category` | String | Yes | Category of the product |

#### Response

**Status:** `201 Created`

**Response Body:**
```json
{
  "orderId": "f7c28bde-4b09-441b-8196-a7169ac8606a",
  "customerName": "Alice",
  "items": [
    {
      "productId": "P1001",
      "quantity": 2,
      "category": "standard"
    }
  ],
  "requestedAt": "2025-06-30T14:00:00Z",
  "createdAt": "2025-08-25T06:16:57.859666Z",
  "status": "CREATED"
}
```

#### Error Responses

**400 Bad Request** - Validation errors
```json
{
  "timestamp": "2025-08-25T06:17:18.192388Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Invalid request data",
  "details": {
    "customerName": "Customer name is required",
    "items": "Order must contain at least one item",
    "requestedAt": "Request timestamp is required"
  }
}
```

**500 Internal Server Error** - Unexpected errors
```json
{
  "timestamp": "2025-08-25T06:17:18.192388Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

## Running the Application

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Kafka broker (optional - for event publishing)

### Build and Run

```bash
# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The application will start on port 8080.

### Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=OrderControllerTest
```

## Example Usage

```bash
# Create a new order
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Alice",
    "items": [
      {
        "productId": "P1001",
        "quantity": 2,
        "category": "standard"
      }
    ],
    "requestedAt": "2025-06-30T14:00:00Z"
  }'
```

## Kafka Events

When an order is created, the service publishes an `OrderCreatedEvent` to the Kafka topic `order-created`. The event contains:

```json
{
  "orderId": "f7c28bde-4b09-441b-8196-a7169ac8606a",
  "customerName": "Alice",
  "items": [
    {
      "productId": "P1001",
      "quantity": 2,
      "category": "standard"
    }
  ],
  "requestedAt": "2025-06-30T14:00:00Z",
  "createdAt": "2025-08-25T06:16:57.859666Z",
  "status": "CREATED",
  "eventType": "ORDER_CREATED",
  "eventTimestamp": "2025-08-25T06:16:57.859666Z"
}
```

### Kafka Configuration

The service is configured to connect to Kafka at `localhost:9092` by default. You can customize this in `application.properties`:

```properties
spring.kafka.bootstrap-servers=localhost:9092
kafka.topic.order-created=order-created
```

**Note:** If Kafka is not available, the order creation will still succeed, but the event publishing will fail gracefully with error logging.

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/bank/poalim/order_service/
│   │       ├── controller/
│   │       │   └── OrderController.java          # REST API endpoints
│   │       ├── dto/
│   │       │   ├── CreateOrderRequestDto.java    # Request DTO
│   │       │   ├── OrderItemDto.java             # Order item DTO
│   │       │   └── OrderResponseDto.java         # Response DTO
│   │       ├── event/
│   │       │   └── OrderCreatedEvent.java        # Kafka event DTO
│   │       ├── exception/
│   │       │   └── GlobalExceptionHandler.java   # Global error handling
│   │       ├── kafka/
│   │       │   └── OrderEventProducer.java       # Kafka producer service
│   │       ├── config/
│   │       │   └── KafkaConfig.java              # Kafka configuration
│   │       ├── service/
│   │       │   ├── OrderService.java             # Service interface
│   │       │   └── OrderServiceImpl.java         # Service implementation
│   │       └── OrderServiceApplication.java      # Main application class
│   └── resources/
│       └── application.properties                # Application configuration
└── test/
    └── java/
        └── com/bank/poalim/order_service/
            ├── controller/
            │   └── OrderControllerTest.java      # API tests
            └── service/
                └── OrderServiceImplTest.java     # Service tests
```
