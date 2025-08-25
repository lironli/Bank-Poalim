package com.bank.poalim.order_service.service;

import com.bank.poalim.order_service.dto.CreateOrderRequestDto;
import com.bank.poalim.order_service.dto.OrderItemDto;
import com.bank.poalim.order_service.dto.OrderResponseDto;
import com.bank.poalim.order_service.event.OrderCreatedEvent;
import com.bank.poalim.order_service.kafka.OrderEventProducer;
import com.bank.poalim.order_service.model.OrderRecord;
import com.bank.poalim.order_service.store.PendingOrderStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    
    @Mock
    private OrderEventProducer orderEventProducer;
    
    @Mock
    private PendingOrderStore pendingOrderStore;
    
    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;
    
    @Captor
    private ArgumentCaptor<OrderRecord> recordCaptor;
    
    private OrderServiceImpl orderService;
    
    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderEventProducer, pendingOrderStore);
        ReflectionTestUtils.setField(orderService, "pendingTtlSeconds", 600L);
    }
    
    @Test
    void createOrder_ValidRequest_SavesPendingAndPublishesEvent() {
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
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getCreatedAt());
        
        // Verify pending save
        verify(pendingOrderStore).savePending(recordCaptor.capture(), eq(600L));
        OrderRecord saved = recordCaptor.getValue();
        assertEquals(response.getOrderId(), saved.getOrderId());
        assertEquals("Alice", saved.getCustomerName());
        assertNotNull(saved.getStatus());
        
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
    void createOrder_KafkaPublishFails_StillReturnsPendingAndSaves() {
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
        assertEquals("PENDING", response.getStatus());
        
        // Verify pending save attempted
        verify(pendingOrderStore).savePending(any(OrderRecord.class), eq(600L));
        // Verify Kafka event was attempted
        verify(orderEventProducer).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
    }
}
