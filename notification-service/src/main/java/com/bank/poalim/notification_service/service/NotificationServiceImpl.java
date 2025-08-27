package com.bank.poalim.notification_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bank.poalim.notification_service.model.InventoryCheckResult;
import com.bank.poalim.notification_service.model.MissingItem;
import com.bank.poalim.notification_service.model.OrderRecord;
import com.bank.poalim.notification_service.store.OrderStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService{
	
	private final OrderStore orderStore;
	
	@Override
	public void processInventoryCheckResult(InventoryCheckResult inventoryCheckResult) {
		
		String orderId = inventoryCheckResult.getOrderId();
	
		log.info("Retrieve the original order from Redis using the orderId");
		Mono<OrderRecord> savedOrder = orderStore.getOrderById(inventoryCheckResult.getOrderId());
		savedOrder.doOnNext(order -> log.info("Retrieved order: {}", order))
			.doOnError(e -> log.info("Failed to retrieve order {} with error: {}", orderId, e));
		
		if(inventoryCheckResult.getApproved()) {
			log.info("Order {} Confirmed!", orderId);
		} else {
			List<MissingItem> missingItems = inventoryCheckResult.getMissingItems();
			log.info("Order {} Rejected due to missing items: {}", orderId, missingItems);
		}
		
	}
   
}
