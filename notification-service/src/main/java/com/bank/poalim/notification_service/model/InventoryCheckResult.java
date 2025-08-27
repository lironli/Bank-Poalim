package com.bank.poalim.notification_service.model;

import java.util.List;

public record InventoryCheckResult (

	String orderId,
    List<MissingItem> missingItems,
    Boolean approved
	
) {}
