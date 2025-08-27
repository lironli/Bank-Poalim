package com.bank.poalim.order_service.event;

import java.time.Instant;
import java.util.List;

import com.bank.poalim.order_service.dto.OrderItemDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class OrderCreatedEvent {
    
    private String orderId;
    private String customerName;
    private List<OrderItemDto> items;
    private Instant requestedAt;
    private Instant createdAt;
    private String status;
    @Builder.Default
    private String eventType = "ORDER_CREATED";
    @Builder.Default
    private Instant eventTimestamp = Instant.now();
}
