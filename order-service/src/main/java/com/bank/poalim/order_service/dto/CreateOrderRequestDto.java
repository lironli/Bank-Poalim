package com.bank.poalim.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

public record CreateOrderRequestDto (
    
    @NotBlank(message = "Customer name is required")
    String customerName,
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    List<OrderItemDto> items,
    
    @NotNull(message = "Request timestamp is required")
    Instant requestedAt

) {}
