package com.bank.poalim.inventory_service.kafka;

import com.bank.poalim.inventory_service.event.OrderValidationEvent;
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
    
    private final KafkaTemplate<String, OrderValidationEvent> kafkaTemplate;
    
    @Value("${kafka.topic.order-validation:order-validation}")
    private String orderValidationTopic;
    
    public CompletableFuture<SendResult<String, OrderValidationEvent>> publishOrderValidationEvent(OrderValidationEvent event) {
        log.info("Publishing order Validation result event to topic '{}': {}", orderValidationTopic, event.getOrderId());
        
        return kafkaTemplate.send(orderValidationTopic, event.getOrderId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Order validation result event published successfully to topic '{}' with key '{}' at partition {} offset {}",
                                orderValidationTopic, event.getOrderId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish order validation result event to topic '{}' with key '{}'", 
                                orderValidationTopic, event.getOrderId(), throwable);
                    }
                });
    }
}
