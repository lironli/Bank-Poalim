package com.bank.poalim.inventory_service.event;

import com.bank.poalim.inventory_service.model.OrderItemDto;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class OrderCreatedEvent {
    private String orderId;
    private String customerName;
    private List<OrderItemDto> items;
    private Instant requestedAt;
    private Instant createdAt;
    private String status;
    private String eventType;
    private Instant eventTimestamp;
}
