package com.bank.poalim.notification_service.model;

public record MissingItem (
	String productId,
    String reason
) {}
