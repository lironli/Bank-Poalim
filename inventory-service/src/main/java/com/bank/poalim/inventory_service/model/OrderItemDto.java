package com.bank.poalim.inventory_service.model;

public record OrderItemDto (
    String productId,
    Integer quantity,
    OrderItemCategory category
) {}
