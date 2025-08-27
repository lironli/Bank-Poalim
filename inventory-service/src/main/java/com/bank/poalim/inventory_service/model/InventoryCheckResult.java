package com.bank.poalim.inventory_service.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class InventoryCheckResult {
    private String orderId;
    private boolean approved;
    private List<ValidationIssue> issues;
    private List<ValidatedItem> validatedItems;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationIssue {
        private String productId;
        private String reason;
        private ValidationIssueType type;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidatedItem {
        private String productId;
        private Integer requestedQuantity;
        private Integer availableQuantity;
        private OrderItemCategory category;
        private boolean available;
    }
    
    public enum ValidationIssueType {
        PRODUCT_NOT_FOUND,
        INSUFFICIENT_QUANTITY,
        EXPIRED_PRODUCT,
        INVALID_CATEGORY,
        PRODUCT_INACTIVE
    }
}
