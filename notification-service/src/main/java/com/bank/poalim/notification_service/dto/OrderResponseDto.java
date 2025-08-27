package com.bank.poalim.notification_service.dto;

import java.time.Instant;
import java.util.List;

public record OrderResponseDto (
    
    String orderId,
    String customerName,
    List<OrderItemDto> items,
    Instant requestedAt,
    Instant createdAt,
    String status

) {}
