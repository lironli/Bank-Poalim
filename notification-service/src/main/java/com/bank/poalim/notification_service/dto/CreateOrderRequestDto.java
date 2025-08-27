package com.bank.poalim.notification_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreateOrderRequestDto {
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemDto> items;
    
    @NotNull(message = "Request timestamp is required")
    private Instant requestedAt;
}
