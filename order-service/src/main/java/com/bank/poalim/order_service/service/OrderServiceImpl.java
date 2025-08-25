package com.bank.poalim.order_service.service;

import com.bank.poalim.order_service.dto.CreateOrderRequestDto;
import com.bank.poalim.order_service.dto.OrderResponseDto;
import com.bank.poalim.order_service.event.OrderCreatedEvent;
import com.bank.poalim.order_service.kafka.OrderEventProducer;
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
    
    @Override
    public OrderResponseDto createOrder(CreateOrderRequestDto request) {
        log.info("Creating order for customer: {}", request.getCustomerName());
        
        // Generate a unique order ID
        String orderId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        
        // Create the response
        OrderResponseDto response = new OrderResponseDto();
        response.setOrderId(orderId);
        response.setCustomerName(request.getCustomerName());
        response.setItems(request.getItems());
        response.setRequestedAt(request.getRequestedAt());
        response.setCreatedAt(createdAt);
        response.setStatus("CREATED");
        
        log.info("Order created successfully with ID: {}", orderId);
        
        // Publish order created event to Kafka
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(orderId)
                    .customerName(request.getCustomerName())
                    .items(request.getItems())
                    .requestedAt(request.getRequestedAt())
                    .createdAt(createdAt)
                    .status("CREATED")
                    .build();
            
            orderEventProducer.publishOrderCreatedEvent(event);
            log.info("Order created event published to Kafka for order ID: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish order created event to Kafka for order ID: {}", orderId, e);
            // Note: We don't fail the order creation if Kafka publishing fails
            // In a production environment, you might want to implement retry logic or dead letter queues
        }
        
        return response;
    }
}
