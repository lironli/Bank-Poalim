package com.bank.poalim.inventory_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationMissingItem {
	private String productId;
    private String reason;
}
