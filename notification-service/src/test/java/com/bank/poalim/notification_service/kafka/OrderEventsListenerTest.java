package com.bank.poalim.notification_service.kafka;

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

import com.bank.poalim.notification_service.event.InventoryCheckResultEvent;
import com.bank.poalim.notification_service.model.MissingItem;
import com.bank.poalim.notification_service.service.NotificationServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderEventsListenerTest {

    @Mock
    private NotificationServiceImpl notificationService;

    @InjectMocks
    private OrderEventsListener orderEventsListener;

    private InventoryCheckResultEvent approvedEvent;
    private InventoryCheckResultEvent rejectedEvent;

    @BeforeEach
    void setUp() {
        // Create approved event
        approvedEvent = InventoryCheckResultEvent.builder()
                .orderId("test-order-123")
                .missingItems(null)
                .approved(true)
                .eventType("INVENTORY_CHECK_RESULT")
                .eventTimestamp(Instant.now())
                .build();

        // Create rejected event
        MissingItem missingItem = new MissingItem();
        missingItem.setProductId("P1001");
        missingItem.setReason("Insufficient quantity");
        
        List<MissingItem> missingItems = Arrays.asList(missingItem);
        rejectedEvent = InventoryCheckResultEvent.builder()
                .orderId("test-order-456")
                .missingItems(missingItems)
                .approved(false)
                .eventType("INVENTORY_CHECK_RESULT")
                .eventTimestamp(Instant.now())
                .build();
    }

    @Test
    void onOrderCreated_WhenApprovedEvent_ShouldProcessSuccessfully() {
        // Arrange
        doNothing().when(notificationService).processInventoryCheckResult(any());

        // Act
        orderEventsListener.onOrderCreated(approvedEvent);

        // Assert
        verify(notificationService).processInventoryCheckResult(argThat(result -> 
            result.getOrderId().equals("test-order-123") &&
            result.getApproved() == true &&
            result.getMissingItems() == null
        ));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onOrderCreated_WhenRejectedEvent_ShouldProcessSuccessfully() {
        // Arrange
        doNothing().when(notificationService).processInventoryCheckResult(any());

        // Act
        orderEventsListener.onOrderCreated(rejectedEvent);

        // Assert
        verify(notificationService).processInventoryCheckResult(argThat(result -> 
            result.getOrderId().equals("test-order-456") &&
            result.getApproved() == false &&
            result.getMissingItems() != null &&
            result.getMissingItems().size() == 1 &&
            result.getMissingItems().get(0).getProductId().equals("P1001")
        ));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onOrderCreated_WhenEventWithEmptyMissingItems_ShouldProcessSuccessfully() {
        // Arrange
        InventoryCheckResultEvent eventWithEmptyItems = InventoryCheckResultEvent.builder()
                .orderId("test-order-789")
                .missingItems(Arrays.asList())
                .approved(false)
                .eventType("INVENTORY_CHECK_RESULT")
                .eventTimestamp(Instant.now())
                .build();

        doNothing().when(notificationService).processInventoryCheckResult(any());

        // Act
        orderEventsListener.onOrderCreated(eventWithEmptyItems);

        // Assert
        verify(notificationService).processInventoryCheckResult(argThat(result -> 
            result.getOrderId().equals("test-order-789") &&
            result.getApproved() == false &&
            result.getMissingItems() != null &&
            result.getMissingItems().isEmpty()
        ));
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onOrderCreated_WhenServiceThrowsException_ShouldHandleGracefully() {
        // Arrange
        doThrow(new RuntimeException("Service error")).when(notificationService).processInventoryCheckResult(any());

        // Act & Assert - The exception should be thrown since we're not handling it gracefully
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            orderEventsListener.onOrderCreated(approvedEvent);
        });

        verify(notificationService).processInventoryCheckResult(any());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onOrderCreated_WhenEventHasMultipleMissingItems_ShouldProcessCorrectly() {
        // Arrange
        MissingItem missingItem1 = new MissingItem();
        missingItem1.setProductId("P1001");
        missingItem1.setReason("Insufficient quantity");
        
        MissingItem missingItem2 = new MissingItem();
        missingItem2.setProductId("P2001");
        missingItem2.setReason("Product expired");
        
        List<MissingItem> multipleMissingItems = Arrays.asList(missingItem1, missingItem2);
        
        InventoryCheckResultEvent eventWithMultipleItems = InventoryCheckResultEvent.builder()
                .orderId("test-order-multi")
                .missingItems(multipleMissingItems)
                .approved(false)
                .eventType("INVENTORY_CHECK_RESULT")
                .eventTimestamp(Instant.now())
                .build();

        doNothing().when(notificationService).processInventoryCheckResult(any());

        // Act
        orderEventsListener.onOrderCreated(eventWithMultipleItems);

        // Assert
        verify(notificationService).processInventoryCheckResult(argThat(result -> 
            result.getOrderId().equals("test-order-multi") &&
            result.getApproved() == false &&
            result.getMissingItems() != null &&
            result.getMissingItems().size() == 2 &&
            result.getMissingItems().get(0).getProductId().equals("P1001") &&
            result.getMissingItems().get(1).getProductId().equals("P2001")
        ));
        verifyNoMoreInteractions(notificationService);
    }
}
