package com.bank.poalim.notification_service.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequestDto(
    
    @NotBlank(message = "Customer name is required")
    String customerName,
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    List<OrderItemDto> items,
    
    @NotNull(message = "Request timestamp is required")
    Instant requestedAt

) {}
