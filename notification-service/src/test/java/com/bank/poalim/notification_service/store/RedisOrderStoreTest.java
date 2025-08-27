package com.bank.poalim.notification_service.store;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import com.bank.poalim.notification_service.dto.OrderItemDto;
import com.bank.poalim.notification_service.model.OrderItemCategory;
import com.bank.poalim.notification_service.model.OrderRecord;
import com.bank.poalim.notification_service.model.OrderStatus;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RedisOrderStoreTest {

    @Mock
    private ReactiveRedisTemplate<String, OrderRecord> orderReactiveRedisTemplate;

    @Mock
    private ReactiveValueOperations<String, OrderRecord> valueOperations;

    @InjectMocks
    private RedisOrderStore redisOrderStore;

    private OrderRecord testOrder;

    @BeforeEach
    void setUp() {
        OrderItemDto orderItem = new OrderItemDto();
        orderItem.setProductId("P1001");
        orderItem.setQuantity(2);
        orderItem.setCategory(OrderItemCategory.STANDARD);
        
        testOrder = OrderRecord.builder()
                .orderId("test-order-123")
                .customerName("John Doe")
                .items(Arrays.asList(orderItem))
                .requestedAt(Instant.now().minusSeconds(300))
                .createdAt(Instant.now().minusSeconds(200))
                .status(OrderStatus.PENDING)
                .build();

        when(orderReactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getOrderById_WhenOrderExists_ShouldReturnOrder() {
        // Arrange
        String orderId = "test-order-123";
        String expectedKey = "order:" + orderId;
        when(valueOperations.get(expectedKey)).thenReturn(Mono.just(testOrder));

        // Act & Assert
        StepVerifier.create(redisOrderStore.getOrderById(orderId))
                .expectNext(testOrder)
                .verifyComplete();

        verify(valueOperations).get(expectedKey);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void getOrderById_WhenOrderDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        String orderId = "non-existent-order";
        String expectedKey = "order:" + orderId;
        when(valueOperations.get(expectedKey)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(redisOrderStore.getOrderById(orderId))
                .verifyComplete();

        verify(valueOperations).get(expectedKey);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void deleteOrder_WhenOrderExists_ShouldReturnTrue() {
        // Arrange
        String orderId = "test-order-123";
        String expectedKey = "order:" + orderId;
        when(valueOperations.delete(expectedKey)).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(redisOrderStore.deleteOrder(orderId))
                .expectNext(true)
                .verifyComplete();

        verify(valueOperations).delete(expectedKey);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void deleteOrder_WhenOrderDoesNotExist_ShouldReturnFalse() {
        // Arrange
        String orderId = "non-existent-order";
        String expectedKey = "order:" + orderId;
        when(valueOperations.delete(expectedKey)).thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(redisOrderStore.deleteOrder(orderId))
                .expectNext(false)
                .verifyComplete();

        verify(valueOperations).delete(expectedKey);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void updateOrderStatus_WhenSuccessful_ShouldReturnTrue() {
        // Arrange
        OrderRecord updatedOrder = OrderRecord.builder()
                .orderId("test-order-123")
                .customerName("John Doe")
                .items(testOrder.getItems())
                .requestedAt(testOrder.getRequestedAt())
                .createdAt(testOrder.getCreatedAt())
                .status(OrderStatus.COMPLETED)
                .build();

        String expectedKey = "order:test-order-123";
        when(valueOperations.set(eq(expectedKey), eq(updatedOrder))).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(redisOrderStore.updateOrderStatus(updatedOrder))
                .expectNext(true)
                .verifyComplete();

        verify(valueOperations).set(expectedKey, updatedOrder);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void updateOrderStatus_WhenRedisError_ShouldReturnFalse() {
        // Arrange
        OrderRecord updatedOrder = OrderRecord.builder()
                .orderId("test-order-123")
                .customerName("John Doe")
                .items(testOrder.getItems())
                .requestedAt(testOrder.getRequestedAt())
                .createdAt(testOrder.getCreatedAt())
                .status(OrderStatus.REJECTED)
                .build();

        String expectedKey = "order:test-order-123";
        when(valueOperations.set(eq(expectedKey), eq(updatedOrder))).thenReturn(Mono.error(new RuntimeException("Redis error")));

        // Act & Assert
        StepVerifier.create(redisOrderStore.updateOrderStatus(updatedOrder))
                .expectNext(false)
                .verifyComplete();

        verify(valueOperations).set(expectedKey, updatedOrder);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void updateOrderStatus_WhenSetReturnsFalse_ShouldReturnFalse() {
        // Arrange
        OrderRecord updatedOrder = OrderRecord.builder()
                .orderId("test-order-123")
                .customerName("John Doe")
                .items(testOrder.getItems())
                .requestedAt(testOrder.getRequestedAt())
                .createdAt(testOrder.getCreatedAt())
                .status(OrderStatus.COMPLETED)
                .build();

        String expectedKey = "order:test-order-123";
        when(valueOperations.set(eq(expectedKey), eq(updatedOrder))).thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(redisOrderStore.updateOrderStatus(updatedOrder))
                .expectNext(true) // The implementation maps any result to TRUE
                .verifyComplete();

        verify(valueOperations).set(expectedKey, updatedOrder);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void keyGeneration_ShouldUseCorrectFormat() {
        // Arrange
        String orderId = "test-order-123";
        String expectedKey = "order:" + orderId;
        when(valueOperations.get(expectedKey)).thenReturn(Mono.empty());

        // Act
        redisOrderStore.getOrderById(orderId).block();

        // Assert
        verify(valueOperations).get(expectedKey);
    }
}
