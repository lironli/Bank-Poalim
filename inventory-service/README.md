# Inventory Service

The Inventory Service is responsible for validating order availability and managing product inventory in the Bank Poalim microservices architecture.

## Features

- **Order Validation**: Validates order items against inventory availability
- **Product Catalog Management**: In-memory product catalog with sample data
- **Category-based Validation Rules**: Different validation logic for each product category
- **Kafka Event Processing**: Listens to order creation events from the order-service
- **REST API**: Exposes product catalog for testing and monitoring

## Validation Rules

### Product Categories

1. **Standard Products**
   - Order can be fulfilled if available quantity >= requested amount
   - Inventory is decremented upon successful order

2. **Perishable Products**
   - Product must not be expired (simulated current date check)
   - Must have sufficient quantity available
   - Inventory is decremented upon successful order

3. **Digital Products**
   - Always considered available
   - Inventory is NOT decremented (unlimited supply)

### Validation Scenarios

- **All products available** → Order approved, inventory updated
- **Some products unavailable** → Order rejected, missing items logged
- **Perishable item expired** → Order rejected
- **Invalid product ID** → Order rejected
- **Unknown category** → Order rejected

## API Endpoints

### Product Catalog

- `GET /api/products` - Get all products
- `GET /api/products/{productId}` - Get specific product
- `POST /api/products` - Add new product
- `PUT /api/products/{productId}/quantity?quantity={newQuantity}` - Update product quantity
- `DELETE /api/products/{productId}` - Remove product

## Sample Data

The service initializes with sample products:

### Standard Products
- `P1001`: Standard Product 1 (50 available)
- `P1002`: Standard Product 2 (10 available)

### Perishable Products
- `P2001`: Fresh Milk (20 available, expires in 7 days)
- `P2002`: Expired Yogurt (5 available, expired yesterday)
- `P2003`: Fresh Bread (15 available, expires in 3 days)

### Digital Products
- `P3001`: Digital Book (1000 available)
- `P3002`: Software License (500 available)

## Running the Service

### Local Development
```bash
./mvnw spring-boot:run
```

### With Docker Compose
```bash
make run_inventory_service
```

## Testing

Run the unit tests:
```bash
./mvnw test
```

### Test Scenarios Covered

1. **All products available** → Order approved
2. **Insufficient quantity** → Order rejected
3. **Expired perishable item** → Order rejected
4. **Product not found** → Order rejected
5. **Digital product** → Always available
6. **Inventory updates** → Only for approved orders

## Kafka Integration

The service listens to the `order-created` topic and processes `OrderCreatedEvent` messages:

1. Receives order creation event
2. Validates all items in the order
3. Updates inventory for approved orders
4. Logs validation results

## Configuration

### Application Properties
- `server.port`: Service port (default: 8081)
- `spring.kafka.bootstrap-servers`: Kafka broker address
- `kafka.topic.order-created`: Kafka topic for order events

### Docker Profile
- `application-docker.properties`: Configuration for Docker Compose environment
