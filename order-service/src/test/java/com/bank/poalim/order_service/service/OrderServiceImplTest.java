package com.bank.poalim.order_service.service;

import com.bank.poalim.order_service.dto.CreateOrderRequestDto;
import com.bank.poalim.order_service.dto.OrderItemDto;
import com.bank.poalim.order_service.dto.OrderResponseDto;
import com.bank.poalim.order_service.event.OrderCreatedEvent;
import com.bank.poalim.order_service.kafka.OrderEventProducer;
import com.bank.poalim.order_service.model.OrderItemCategory;
import com.bank.poalim.order_service.model.OrderRecord;
import com.bank.poalim.order_service.store.PendingOrderStore;
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
    }
    
    @Test
    void createOrder_ValidRequest_SavesPendingAndPublishesEvent() {
        // Given
        
        OrderItemDto item = new OrderItemDto(
        	"P1001",
        	2,
        	OrderItemCategory.STANDARD
        );
        
        CreateOrderRequestDto request = new CreateOrderRequestDto(
            	"Alice",
            	List.of(item),
            	Instant.parse("2025-06-30T14:00:00Z")
            );
        
        when(orderEventProducer.publishOrderCreatedEvent(any(OrderCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // When
        OrderResponseDto response = orderService.createOrder(request);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.orderId());
        assertEquals("Alice", response.customerName());
        assertEquals("PENDING", response.status());
        assertNotNull(response.createdAt());
        
        // Verify pending save
        verify(pendingOrderStore).savePending(recordCaptor.capture());
        OrderRecord saved = recordCaptor.getValue();
        assertEquals(response.orderId(), saved.getOrderId());
        assertEquals("Alice", saved.getCustomerName());
        assertNotNull(saved.getStatus());
        
        // Verify Kafka event was published
        verify(orderEventProducer).publishOrderCreatedEvent(eventCaptor.capture());
        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        
        assertEquals(response.orderId(), capturedEvent.getOrderId());
        assertEquals("Alice", capturedEvent.getCustomerName());
        assertEquals("CREATED", capturedEvent.getStatus());
        assertEquals("ORDER_CREATED", capturedEvent.getEventType());
        assertNotNull(capturedEvent.getEventTimestamp());
    }
    
    @Test
    void createOrder_KafkaPublishFails_StillReturnsPendingAndSaves() {
        // Given
    	
    	OrderItemDto item = new OrderItemDto(
        	"P1001",
        	2,
        	OrderItemCategory.DIGITAL
        );
        
        CreateOrderRequestDto request = new CreateOrderRequestDto(
        	"Alice",
        	List.of(item),
        	Instant.parse("2025-06-30T14:00:00Z")
        );
        
        
        when(orderEventProducer.publishOrderCreatedEvent(any(OrderCreatedEvent.class)))
                .thenThrow(new RuntimeException("Kafka connection failed"));
        
        // When
        OrderResponseDto response = orderService.createOrder(request);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.orderId());
        assertEquals("Alice", response.customerName());
        assertEquals("PENDING", response.status());
        
        // Verify pending save attempted
        verify(pendingOrderStore).savePending(any(OrderRecord.class));
        // Verify Kafka event was attempted
        verify(orderEventProducer).publishOrderCreatedEvent(any(OrderCreatedEvent.class));
    }
}
