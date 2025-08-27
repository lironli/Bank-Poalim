package com.bank.poalim.inventory_service.model;

public record ValidationMissingItem (
	String productId,
    String reason
) {}
