package com.bank.poalim.order_service.service;

import com.bank.poalim.order_service.dto.CreateOrderRequestDto;
import com.bank.poalim.order_service.dto.OrderResponseDto;

public interface OrderService {
    
    OrderResponseDto createOrder(CreateOrderRequestDto request);
}
