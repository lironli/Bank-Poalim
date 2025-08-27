package com.bank.poalim.order_service.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

@Builder
public record OrderResponseDto (
    
    String orderId,
    String customerName,
    List<OrderItemDto> items,
    Instant requestedAt,
    Instant createdAt,
    String status
    
) {}
