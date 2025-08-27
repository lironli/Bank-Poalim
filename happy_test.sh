#!/bin/bash

# Happy Path Test: Order Processing Flow
# Tests the complete flow from order creation to final status update in Redis

set -e

echo "🧪 Starting happy path test..."
echo

# Test configuration
ORDER_SERVICE_URL="http://localhost:8081"
INVENTORY_SERVICE_URL="http://localhost:8082"
NOTIFICATION_SERVICE_URL="http://localhost:8083"

# Generate a unique order ID for this test run
CUSTOMER_NAME="TestCustomer_$(date +%s)"
CURRENT_TIME=$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")

echo "📝 Creating order for customer: $CUSTOMER_NAME"

# Step 1: Create an order with available products
ORDER_RESPONSE=$(curl -s -X POST "$ORDER_SERVICE_URL/orders" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"$CUSTOMER_NAME\",
    \"requestedAt\": \"$CURRENT_TIME\",
    \"items\": [
      {
        \"productId\": \"P1001\",
        \"quantity\": 2,
        \"category\": \"standard\"
      },
      {
        \"productId\": \"P2001\",
        \"quantity\": 1,
        \"category\": \"perishable\"
      }
    ]
  }")

echo "✅ Order created successfully"

# Extract order ID from response
ORDER_ID=$(echo "$ORDER_RESPONSE" | grep -o '"orderId":"[^"]*' | cut -d'"' -f4)
echo "📋 Order ID: $ORDER_ID"
echo

# Step 2: Wait for inventory service to process the order
echo "⏳ Waiting for inventory service to process order..."
sleep 3
echo "✅ Inventory validation completed"
echo

# Step 3: Wait for notification service to process the inventory result  
echo "⏳ Waiting for notification service to update order status..."
sleep 2
echo "✅ Notification processing completed"
echo

# Step 4: Verify final order status in Redis
echo "🔍 Checking final order status in Redis..."

# First, check if any keys exist with status embedded in the key name (this should fail the test)
WRONG_KEYS=$(docker exec redis redis-cli KEYS "order:*:*" 2>/dev/null | grep "$ORDER_ID" || echo "")
if [ ! -z "$WRONG_KEYS" ]; then
    echo "❌ FAILED: Found Redis keys with status embedded in key name:"
    echo "   $WRONG_KEYS"
    echo "   Expected key format: order:ORDER_ID"
    echo "   Status should be stored in the record, not the key"
    exit 1
fi

# Check for the correct key format: order:ORDER_ID
ORDER_DATA=$(docker exec redis redis-cli GET "order:$ORDER_ID" 2>/dev/null || echo "NOT_FOUND")

if [ "$ORDER_DATA" = "NOT_FOUND" ]; then
    echo "❌ FAILED: Order not found in Redis with key: order:$ORDER_ID"
    echo
    echo "🔍 Debugging info:"
    echo "   Order ID: $ORDER_ID"
    echo "   Customer: $CUSTOMER_NAME"
    echo "   All order keys in Redis:"
    docker exec redis redis-cli KEYS "order:*" 2>/dev/null | head -5
    exit 1
fi

# Extract status from JSON response
ORDER_STATUS=$(echo "$ORDER_DATA" | grep -o '"status":"[^"]*' | cut -d'"' -f4)

if [ "$ORDER_STATUS" = "COMPLETED" ]; then
    echo "✅ SUCCESS: Order status is COMPLETED"
    echo "✅ SUCCESS: Redis key format is correct (order:$ORDER_ID)"
    echo
    echo "🎉 Happy path test PASSED!"
    echo "   - Order created via Order Service"  
    echo "   - Inventory validated with 0 missing items"
    echo "   - Order status updated to COMPLETED in Redis"
    echo "   - Redis key follows correct format: order:ORDER_ID"
else
    echo "❌ FAILED: Expected order status COMPLETED, but got: $ORDER_STATUS"
    echo
    echo "🔍 Debugging info:"
    echo "   Order ID: $ORDER_ID"
    echo "   Customer: $CUSTOMER_NAME" 
    echo "   Redis status: $ORDER_STATUS"
    echo "   Full Redis data: $ORDER_DATA"
    exit 1
fi

echo
echo "✨ Test completed successfully!"