package com.bank.poalim.inventory_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String productId;
    private String name;
    private OrderItemCategory category;
    private Integer availableQuantity;
    private LocalDate expirationDate; // null for non-perishable items
    private boolean active;
}
