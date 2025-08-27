package com.bank.poalim.order_service.service;

import com.bank.poalim.order_service.dto.CreateOrderRequestDto;
import com.bank.poalim.order_service.dto.OrderResponseDto;
import com.bank.poalim.order_service.event.OrderCreatedEvent;
import com.bank.poalim.order_service.kafka.OrderEventProducer;
import com.bank.poalim.order_service.model.OrderRecord;
import com.bank.poalim.order_service.model.OrderStatus;
import com.bank.poalim.order_service.store.PendingOrderStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderEventProducer orderEventProducer;
    private final PendingOrderStore pendingOrderStore;
    
    @Override
    public OrderResponseDto createOrder(CreateOrderRequestDto request) {
        log.info("Creating order for customer: {}", request.getCustomerName());
        
        // Generate a unique order ID
        String orderId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
                
        // Save PENDING order in Redis
        OrderRecord record = OrderRecord.builder()
                .orderId(orderId)
                .customerName(request.getCustomerName())
                .items(request.getItems())
                .requestedAt(request.getRequestedAt())
                .createdAt(createdAt)
                .status(OrderStatus.PENDING)
                .build();
        pendingOrderStore.savePending(record);
                
        // Create the response (reflect current PENDING status)
        OrderResponseDto response = new OrderResponseDto();
        response.setOrderId(orderId);
        response.setCustomerName(request.getCustomerName());
        response.setItems(request.getItems());
        response.setRequestedAt(request.getRequestedAt());
        response.setCreatedAt(createdAt);
        response.setStatus("PENDING");
        
        // Publish order created event to Kafka
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(orderId)
                    .customerName(request.getCustomerName())
                    .items(request.getItems())
                    .requestedAt(createdAt.isAfter(request.getRequestedAt()) ? request.getRequestedAt() : request.getRequestedAt())
                    .createdAt(createdAt)
                    .status("CREATED")
                    .build();
            
            orderEventProducer.publishOrderCreatedEvent(event);
            log.info("Order created event published to Kafka for order ID: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish order created event to Kafka for order ID: {}", orderId, e);
            // order remains PENDING in Redis, Ideally we should have a retry mechanism to publish the event again
        }
        
        return response;
    }
}
