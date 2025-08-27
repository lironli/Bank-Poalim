package com.bank.poalim.notification_service.store;

import com.bank.poalim.notification_service.model.OrderRecord;

import reactor.core.publisher.Mono;

public interface OrderStore {
    
	Mono<OrderRecord> getOrderById(String orderId);
	
	Mono<OrderRecord> retrieveAndDeleteOrder(String orderId);
	
}
