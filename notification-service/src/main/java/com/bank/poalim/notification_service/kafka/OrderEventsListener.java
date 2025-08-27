package com.bank.poalim.notification_service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.bank.poalim.notification_service.event.InventoryCheckResultEvent;
import com.bank.poalim.notification_service.model.InventoryCheckResult;
import com.bank.poalim.notification_service.service.NotificationServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsListener {

    private final NotificationServiceImpl notificationService;

    
    @KafkaListener(topics = "${kafka.topic.inventory-check-result:inventory-check-result}", containerFactory = "orderKafkaListenerContainerFactory")
    public void onOrderCreated(@Payload InventoryCheckResultEvent event) {
        
    	log.info("Notification received InventoryCheckResultEvent id={} isApproved={}",
                event.getOrderId(),
                event.getApproved());
        
        
    	InventoryCheckResult inventoryCheckResult = new InventoryCheckResult(
    			event.getOrderId(),
    			event.getMissingItems(),
    			event.getApproved()
    			);
        
    	log.info("Starting to process Inventory check result of orderID={}", event.getOrderId());
    	notificationService.processInventoryCheckResult(inventoryCheckResult);
    	
    }
}
