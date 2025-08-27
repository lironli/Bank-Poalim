package com.bank.poalim.notification_service.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryCheckResult {

	private String orderId;
    private List<MissingItem> missingItems;
    private Boolean approved;
	
}
