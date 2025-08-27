package com.bank.poalim.order_service.model;

import com.bank.poalim.order_service.dto.OrderItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRecord implements Serializable {
    private String orderId;
    private String customerName;
    private List<OrderItemDto> items;
    private Instant requestedAt;
    private Instant createdAt;
    private OrderStatus status;
}
