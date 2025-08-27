package com.bank.poalim.notification_service.service;

import com.bank.poalim.notification_service.model.InventoryCheckResult;

public interface NotificationService {
	
	void processInventoryCheckResult(InventoryCheckResult inventoryCheckResult);
	
}
