package com.bank.poalim.notification_service.dto;

import com.bank.poalim.notification_service.model.OrderItemCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

public record OrderItemDto (
    
    @NotBlank(message = "Product ID is required")
    String productId,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity,
    
    @NotNull(message = "Category is required")
    OrderItemCategory category
    
) {}
