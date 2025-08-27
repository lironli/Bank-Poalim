package com.bank.poalim.notification_service.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class OrderResponseDto {
    
    private String orderId;
    private String customerName;
    private List<OrderItemDto> items;
    private Instant requestedAt;
    private Instant createdAt;
    private String status;
}
