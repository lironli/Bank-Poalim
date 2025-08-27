package com.bank.poalim.notification_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bank.poalim.notification_service.model.InventoryCheckResult;
import com.bank.poalim.notification_service.model.MissingItem;
import com.bank.poalim.notification_service.model.OrderRecord;
import com.bank.poalim.notification_service.model.OrderStatus;
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
		
		OrderRecord savedOrder = orderStore.getOrderById(orderId).block();
		log.info("Retrieved order: {}", savedOrder);

		if (savedOrder != null) {
			// Update the order status to COMPLETED or REJECTED based on approval
			OrderStatus newStatus = inventoryCheckResult.getApproved() ? OrderStatus.COMPLETED : OrderStatus.REJECTED;
			
			OrderRecord updatedOrder = OrderRecord.builder()
					.orderId(savedOrder.getOrderId())
					.customerName(savedOrder.getCustomerName())
					.items(savedOrder.getItems())
					.requestedAt(savedOrder.getRequestedAt())
					.createdAt(savedOrder.getCreatedAt())
					.status(newStatus)  // Set status based on approval
					.build();
			
			Boolean updated = orderStore.updateOrderStatus(updatedOrder).block();
			if (Boolean.TRUE.equals(updated)) {
				log.info("Successfully updated order {} status to {}", orderId, newStatus);
			} else {
				log.error("Failed to update order {} status", orderId);
			}
		} else {
			log.error("Order {} not found in Redis", orderId);
		}
		
		if(inventoryCheckResult.getApproved()) {
			log.info("Order {} Confirmed!", orderId);
		} else {
			List<MissingItem> missingItems = inventoryCheckResult.getMissingItems();
			log.info("Order {} Rejected due to missing items: {}", orderId, missingItems);
		}
		
	}
   
}
