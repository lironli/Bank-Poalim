package com.bank.poalim.inventory_service.event;

import java.time.Instant;
import java.util.List;

import com.bank.poalim.inventory_service.model.OrderItemDto;

public record OrderCreatedEvent (
    String orderId,
    String customerName,
    List<OrderItemDto> items,
    Instant requestedAt,
    Instant createdAt,
    String status,
    String eventType,
    Instant eventTimestamp
) {}
