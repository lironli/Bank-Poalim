package com.bank.poalim.notification_service.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import com.bank.poalim.notification_service.dto.OrderItemDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class OrderRecord implements Serializable {
    private String orderId;
    private String customerName;
    private List<OrderItemDto> items;
    private Instant requestedAt;
    private Instant createdAt;
    private OrderStatus status;
}
