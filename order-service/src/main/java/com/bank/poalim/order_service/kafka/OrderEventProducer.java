package com.bank.poalim.order_service.kafka;

import com.bank.poalim.order_service.event.OrderCreatedEvent;
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
public class OrderEventProducer {
    
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    
    @Value("${kafka.topic.order-created:order-created}")
    private String orderCreatedTopic;
    
    public CompletableFuture<SendResult<String, OrderCreatedEvent>> publishOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Publishing order created event to topic '{}': {}", orderCreatedTopic, event.getOrderId());
        
        return kafkaTemplate.send(orderCreatedTopic, event.getOrderId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Order created event published successfully to topic '{}' with key '{}' at partition {} offset {}",
                                orderCreatedTopic, event.getOrderId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish order created event to topic '{}' with key '{}'", 
                                orderCreatedTopic, event.getOrderId(), throwable);
                    }
                });
    }
}
