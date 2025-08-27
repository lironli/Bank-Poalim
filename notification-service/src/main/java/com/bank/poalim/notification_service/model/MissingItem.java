package com.bank.poalim.notification_service.model;

import lombok.Data;

@Data
public class MissingItem {
	private String productId;
    private String reason;
}
