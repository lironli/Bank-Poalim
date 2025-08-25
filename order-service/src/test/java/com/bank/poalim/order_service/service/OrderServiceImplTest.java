package com.bank.poalim.order_service.service;

import com.bank.poalim.order_service.dto.CreateOrderRequestDto;
import com.bank.poalim.order_service.dto.OrderItemDto;
import com.bank.poalim.order_service.dto.OrderResponseDto;
import com.bank.poalim.order_service.event.OrderCreatedEvent;
import com.bank.poalim.order_service.kafka.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    
    @Mock
    private OrderEventProducer orderEventProducer;
    
    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;
    
    private OrderServiceImpl orderService;
    
    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderEventProducer);
    }
    
    @Test
    void createOrder_ValidRequest_PublishesEventToKafka() {
        // Given
        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setCustomerName("Alice");
        request.setRequestedAt(Instant.parse("2025-06-30T14:00:00Z"));
        
        OrderItemDto item = new OrderItemDto();
        item.setProductId("P1001");
        item.setQuantity(2);
        item.setCategory("standard");
        request.setItems(List.of(item));
        
        when(orderEventProducer.publishOrderCreatedEvent(any(OrderCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // When
        OrderResponseDto response = orderService.createOrder(request);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals("Alice", response.getCustomerName());
        assertEquals("CREATED", response.getStatus());
        assertNotNull(response.getCreatedAt());
        
        // Verify Kafka event was published
        verify(orderEventProducer).publishOrderCreatedEvent(eventCaptor.capture());
        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        
        assertEquals(response.getOrderId(), capturedEvent.getOrderId());
        assertEquals("Alice", capturedEvent.getCustomerName());
        assertEquals("CREATED", capturedEvent.getStatus());
        assertEquals("ORDER_CREATED", capturedEvent.getEventType());
        assertNotNull(capturedEvent.getEventTimestamp());
    }
    
    @Test
    void createOrder_KafkaPublishFails_StillReturnsOrder() {
        // Given
        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setCustomerName("Alice");
        request.setRequestedAt(Instant.parse("2025-06-30T14:00:00Z"));
        
        OrderItemDto item = new OrderItemDto();
        item.setProductId("P1001");
        item.setQuantity(2);
        item.setCategory("standard");
        request.setItems(List.of(item));
        
        when(orderEventProducer.publishOrderCreatedEvent(any(OrderCreatedEvent.class)))
                .thenThrow(new RuntimeException("Kafka connection failed"));
        
        // When
        OrderResponseDto response = orderService.createOrder(request);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals("Alice", response.getCustomerName());
        assertEquals("CREATED", response.getStatus());
        
        // Verify Kafka event was attempted
        verify(orderEventProducer).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
    }
}
