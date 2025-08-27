package com.bank.poalim.order_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.bank.poalim.order_service.dto.CreateOrderRequestDto;
import com.bank.poalim.order_service.dto.OrderItemDto;
import com.bank.poalim.order_service.dto.OrderResponseDto;
import com.bank.poalim.order_service.model.OrderItemCategory;
import com.bank.poalim.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    	OrderItemDto item = new OrderItemDto(
        	"P1001",
        	2,
        	OrderItemCategory.STANDARD
        );
    	
    	CreateOrderRequestDto request = new CreateOrderRequestDto(
        	"Alice",
        	List.of(item),
        	Instant.parse("2025-06-30T14:00:00Z")
        );
        
        OrderResponseDto response = OrderResponseDto.builder()
        	.orderId("test-order-id")
        	.customerName("Alice")
        	.items(request.items())
        	.requestedAt(request.requestedAt())
        	.createdAt(Instant.now())
        	.status("CREATED")
        	.build();
        
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
        CreateOrderRequestDto request = new CreateOrderRequestDto(null, null, null);
        // Missing required fields
        
        // When & Then
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
