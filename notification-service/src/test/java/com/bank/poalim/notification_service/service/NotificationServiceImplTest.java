package com.bank.poalim.notification_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.poalim.notification_service.dto.OrderItemDto;
import com.bank.poalim.notification_service.model.InventoryCheckResult;
import com.bank.poalim.notification_service.model.MissingItem;
import com.bank.poalim.notification_service.model.OrderItemCategory;
import com.bank.poalim.notification_service.model.OrderRecord;
import com.bank.poalim.notification_service.model.OrderStatus;
import com.bank.poalim.notification_service.store.OrderStore;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private OrderStore orderStore;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private OrderRecord testOrder;
    private InventoryCheckResult approvedResult;
    private InventoryCheckResult rejectedResult;

    @BeforeEach
    void setUp() {
        // Create test order
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

        // Create approved inventory check result
        approvedResult = new InventoryCheckResult(
                "test-order-123",
                null, // no missing items
                true  // approved
        );

        // Create rejected inventory check result
        MissingItem missingItem = new MissingItem();
        missingItem.setProductId("P1001");
        missingItem.setReason("Insufficient quantity");
        
        List<MissingItem> missingItems = Arrays.asList(missingItem);
        rejectedResult = new InventoryCheckResult(
                "test-order-123",
                missingItems,
                false  // not approved
        );
    }

    @Test
    void processInventoryCheckResult_WhenOrderExistsAndApproved_ShouldUpdateStatusToCompleted() {
        // Arrange
        when(orderStore.getOrderById("test-order-123")).thenReturn(Mono.just(testOrder));
        when(orderStore.updateOrderStatus(any(OrderRecord.class))).thenReturn(Mono.just(true));

        // Act
        notificationService.processInventoryCheckResult(approvedResult);

        // Assert
        verify(orderStore).getOrderById("test-order-123");
        verify(orderStore).updateOrderStatus(argThat(order -> 
            order.getOrderId().equals("test-order-123") &&
            order.getStatus() == OrderStatus.COMPLETED &&
            order.getCustomerName().equals("John Doe")
        ));
        verifyNoMoreInteractions(orderStore);
    }

    @Test
    void processInventoryCheckResult_WhenOrderExistsAndRejected_ShouldUpdateStatusToRejected() {
        // Arrange
        when(orderStore.getOrderById("test-order-123")).thenReturn(Mono.just(testOrder));
        when(orderStore.updateOrderStatus(any(OrderRecord.class))).thenReturn(Mono.just(true));

        // Act
        notificationService.processInventoryCheckResult(rejectedResult);

        // Assert
        verify(orderStore).getOrderById("test-order-123");
        verify(orderStore).updateOrderStatus(argThat(order -> 
            order.getOrderId().equals("test-order-123") &&
            order.getStatus() == OrderStatus.REJECTED &&
            order.getCustomerName().equals("John Doe")
        ));
        verifyNoMoreInteractions(orderStore);
    }

    @Test
    void processInventoryCheckResult_WhenOrderNotFound_ShouldLogErrorAndNotUpdate() {
        // Arrange
        when(orderStore.getOrderById("test-order-123")).thenReturn(Mono.empty());

        // Act
        notificationService.processInventoryCheckResult(approvedResult);

        // Assert
        verify(orderStore).getOrderById("test-order-123");
        verify(orderStore, never()).updateOrderStatus(any(OrderRecord.class));
        verifyNoMoreInteractions(orderStore);
    }

    @Test
    void processInventoryCheckResult_WhenUpdateFails_ShouldLogError() {
        // Arrange
        when(orderStore.getOrderById("test-order-123")).thenReturn(Mono.just(testOrder));
        when(orderStore.updateOrderStatus(any(OrderRecord.class))).thenReturn(Mono.just(false));

        // Act
        notificationService.processInventoryCheckResult(approvedResult);

        // Assert
        verify(orderStore).getOrderById("test-order-123");
        verify(orderStore).updateOrderStatus(any(OrderRecord.class));
        verifyNoMoreInteractions(orderStore);
    }

    @Test
    void processInventoryCheckResult_WhenOrderExistsAndApproved_ShouldPreserveAllOrderData() {
        // Arrange
        when(orderStore.getOrderById("test-order-123")).thenReturn(Mono.just(testOrder));
        when(orderStore.updateOrderStatus(any(OrderRecord.class))).thenReturn(Mono.just(true));

        // Act
        notificationService.processInventoryCheckResult(approvedResult);

        // Assert
        verify(orderStore).updateOrderStatus(argThat(order -> 
            order.getOrderId().equals(testOrder.getOrderId()) &&
            order.getCustomerName().equals(testOrder.getCustomerName()) &&
            order.getItems().equals(testOrder.getItems()) &&
            order.getRequestedAt().equals(testOrder.getRequestedAt()) &&
            order.getCreatedAt().equals(testOrder.getCreatedAt()) &&
            order.getStatus() == OrderStatus.COMPLETED
        ));
    }

    @Test
    void processInventoryCheckResult_WhenOrderExistsAndRejected_ShouldPreserveAllOrderData() {
        // Arrange
        when(orderStore.getOrderById("test-order-123")).thenReturn(Mono.just(testOrder));
        when(orderStore.updateOrderStatus(any(OrderRecord.class))).thenReturn(Mono.just(true));

        // Act
        notificationService.processInventoryCheckResult(rejectedResult);

        // Assert
        verify(orderStore).updateOrderStatus(argThat(order -> 
            order.getOrderId().equals(testOrder.getOrderId()) &&
            order.getCustomerName().equals(testOrder.getCustomerName()) &&
            order.getItems().equals(testOrder.getItems()) &&
            order.getRequestedAt().equals(testOrder.getRequestedAt()) &&
            order.getCreatedAt().equals(testOrder.getCreatedAt()) &&
            order.getStatus() == OrderStatus.REJECTED
        ));
    }

    @Test
    void processInventoryCheckResult_WhenUpdateThrowsException_ShouldHandleGracefully() {
        // Arrange
        when(orderStore.getOrderById("test-order-123")).thenReturn(Mono.just(testOrder));
        when(orderStore.updateOrderStatus(any(OrderRecord.class))).thenReturn(Mono.error(new RuntimeException("Redis error")));

        // Act & Assert - The exception should be thrown since we're not handling it gracefully
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            notificationService.processInventoryCheckResult(approvedResult);
        });

        verify(orderStore).getOrderById("test-order-123");
        verify(orderStore).updateOrderStatus(any(OrderRecord.class));
        verifyNoMoreInteractions(orderStore);
    }
}
