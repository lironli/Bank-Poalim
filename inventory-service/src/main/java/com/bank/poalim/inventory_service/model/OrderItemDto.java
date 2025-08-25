package com.bank.poalim.inventory_service.model;

import lombok.Data;

@Data
public class OrderItemDto {
    private String productId;
    private Integer quantity;
    private OrderItemCategory category;
}
