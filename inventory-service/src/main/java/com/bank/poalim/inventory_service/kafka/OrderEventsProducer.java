package com.bank.poalim.inventory_service.kafka;

import com.bank.poalim.inventory_service.event.InventoryCheckResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventsProducer {
    
    private final KafkaTemplate<String, InventoryCheckResultEvent> kafkaTemplate;
    
    @Value("${kafka.topic.inventory-check:inventory-check-result}")
    private String inventoryCheckResultTopic;
    
    public CompletableFuture<SendResult<String, InventoryCheckResultEvent>> publishInventoryCheckResultEvent(InventoryCheckResultEvent event) {
        log.info("Publishing inventory check result event to topic '{}': {}", inventoryCheckResultTopic, event.getOrderId());
        
        return kafkaTemplate.send(inventoryCheckResultTopic, event.getOrderId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Inventory check result  event published successfully to topic '{}' with key '{}' at partition {} offset {}",
                                inventoryCheckResultTopic, event.getOrderId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish inventory check result event to topic '{}' with key '{}'", 
                                inventoryCheckResultTopic, event.getOrderId(), throwable);
                    }
                });
    }
}
