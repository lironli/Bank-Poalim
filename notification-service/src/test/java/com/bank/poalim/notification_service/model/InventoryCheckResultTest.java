package com.bank.poalim.notification_service.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class InventoryCheckResultTest {

    @Test
    void constructor_WithValidData_ShouldCreateInstance() {
        // Arrange
        String orderId = "test-order-123";
        MissingItem missingItem = new MissingItem();
        missingItem.setProductId("P1001");
        missingItem.setReason("Insufficient quantity");
        
        List<MissingItem> missingItems = Arrays.asList(missingItem);
        Boolean approved = false;

        // Act
        InventoryCheckResult result = new InventoryCheckResult(orderId, missingItems, approved);

        // Assert
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getMissingItems()).isEqualTo(missingItems);
        assertThat(result.getApproved()).isEqualTo(approved);
    }

    @Test
    void constructor_WithNullMissingItems_ShouldCreateInstance() {
        // Arrange
        String orderId = "test-order-123";
        Boolean approved = true;

        // Act
        InventoryCheckResult result = new InventoryCheckResult(orderId, null, approved);

        // Assert
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getMissingItems()).isNull();
        assertThat(result.getApproved()).isEqualTo(approved);
    }

    @Test
    void constructor_WithEmptyMissingItems_ShouldCreateInstance() {
        // Arrange
        String orderId = "test-order-123";
        List<MissingItem> missingItems = Arrays.asList();
        Boolean approved = false;

        // Act
        InventoryCheckResult result = new InventoryCheckResult(orderId, missingItems, approved);

        // Assert
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getMissingItems()).isEmpty();
        assertThat(result.getApproved()).isEqualTo(approved);
    }

    @Test
    void equals_WithSameData_ShouldReturnTrue() {
        // Arrange
        String orderId = "test-order-123";
        MissingItem missingItem = new MissingItem();
        missingItem.setProductId("P1001");
        missingItem.setReason("Insufficient quantity");
        
        List<MissingItem> missingItems = Arrays.asList(missingItem);
        Boolean approved = false;

        InventoryCheckResult result1 = new InventoryCheckResult(orderId, missingItems, approved);
        InventoryCheckResult result2 = new InventoryCheckResult(orderId, missingItems, approved);

        // Act & Assert
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void equals_WithDifferentData_ShouldReturnFalse() {
        // Arrange
        InventoryCheckResult result1 = new InventoryCheckResult("order-1", null, true);
        InventoryCheckResult result2 = new InventoryCheckResult("order-2", null, true);

        // Act & Assert
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void hashCode_WithSameData_ShouldReturnSameHashCode() {
        // Arrange
        String orderId = "test-order-123";
        MissingItem missingItem = new MissingItem();
        missingItem.setProductId("P1001");
        missingItem.setReason("Insufficient quantity");
        
        List<MissingItem> missingItems = Arrays.asList(missingItem);
        Boolean approved = false;

        InventoryCheckResult result1 = new InventoryCheckResult(orderId, missingItems, approved);
        InventoryCheckResult result2 = new InventoryCheckResult(orderId, missingItems, approved);

        // Act & Assert
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void toString_ShouldContainAllFields() {
        // Arrange
        String orderId = "test-order-123";
        MissingItem missingItem = new MissingItem();
        missingItem.setProductId("P1001");
        missingItem.setReason("Insufficient quantity");
        
        List<MissingItem> missingItems = Arrays.asList(missingItem);
        Boolean approved = false;

        InventoryCheckResult result = new InventoryCheckResult(orderId, missingItems, approved);

        // Act
        String toString = result.toString();

        // Assert
        assertThat(toString).contains(orderId);
        assertThat(toString).contains("P1001");
        assertThat(toString).contains("Insufficient quantity");
        assertThat(toString).contains("false");
    }
}
