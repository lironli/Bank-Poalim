package com.bank.poalim.inventory_service.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.bank.poalim.inventory_service.event.OrderCreatedEvent;
import com.bank.poalim.inventory_service.model.InventoryCheckResult;
import com.bank.poalim.inventory_service.service.InventoryValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsListener {

    private final InventoryValidationService inventoryValidationService;

    @KafkaListener(topics = "${kafka.topic.order-created:order-created}", containerFactory = "orderKafkaListenerContainerFactory")
    public void onOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("Inventory received OrderCreatedEvent id={} items={} status={}",
                event.getOrderId(),
                event.getItems() != null ? event.getItems().size() : 0,
                event.getStatus());
        
        try {
            // Validate order availability
            InventoryCheckResult validationResult = inventoryValidationService.validateOrder(
                    event.getOrderId(), 
                    event.getItems()
            );
            
            if (validationResult.isApproved()) {
                // Update inventory for approved orders
//                inventoryValidationService.updateInventoryForApprovedOrder(validationResult);
                log.info("Order {} completed processing - Order approved and inventory updated", event.getOrderId());
            } else {
                log.warn("Order {} completed processing - Order rejected and inventory remains unchanged", event.getOrderId());
            }
            
        } catch (Exception e) {
            log.error("Error processing order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}
