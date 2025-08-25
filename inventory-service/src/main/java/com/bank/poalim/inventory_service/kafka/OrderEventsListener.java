package com.bank.poalim.inventory_service.kafka;

import com.bank.poalim.inventory_service.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventsListener {

    @KafkaListener(topics = "${kafka.topic.order-created:order-created}", containerFactory = "orderKafkaListenerContainerFactory")
    public void onOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("Inventory received OrderCreatedEvent id={} items={} status={}",
                event.getOrderId(),
                event.getItems() != null ? event.getItems().size() : 0,
                event.getStatus());
        // TODO: decrement inventory, validate availability, publish follow-up events, etc.
    }
}
