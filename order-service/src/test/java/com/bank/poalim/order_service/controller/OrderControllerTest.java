package com.bank.poalim.order_service.controller;

import com.bank.poalim.order_service.dto.CreateOrderRequestDto;
import com.bank.poalim.order_service.dto.OrderItemDto;
import com.bank.poalim.order_service.dto.OrderResponseDto;
import com.bank.poalim.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private OrderService orderService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void createOrder_ValidRequest_ReturnsCreatedOrder() throws Exception {
        // Given
        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setCustomerName("Alice");
        request.setRequestedAt(Instant.parse("2025-06-30T14:00:00Z"));
        
        OrderItemDto item = new OrderItemDto();
        item.setProductId("P1001");
        item.setQuantity(2);
        item.setCategory("standard");
        request.setItems(List.of(item));
        
        OrderResponseDto response = new OrderResponseDto();
        response.setOrderId("test-order-id");
        response.setCustomerName("Alice");
        response.setItems(request.getItems());
        response.setRequestedAt(request.getRequestedAt());
        response.setCreatedAt(Instant.now());
        response.setStatus("CREATED");
        
        when(orderService.createOrder(any(CreateOrderRequestDto.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("test-order-id"))
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }
    
    @Test
    void createOrder_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        CreateOrderRequestDto request = new CreateOrderRequestDto();
        // Missing required fields
        
        // When & Then
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
