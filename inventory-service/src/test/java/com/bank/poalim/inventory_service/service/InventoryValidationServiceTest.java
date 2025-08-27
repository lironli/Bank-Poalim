package com.bank.poalim.inventory_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.poalim.inventory_service.kafka.OrderEventsProducer;
import com.bank.poalim.inventory_service.model.InventoryCheckResult;
import com.bank.poalim.inventory_service.model.OrderItemCategory;
import com.bank.poalim.inventory_service.model.OrderItemDto;
import com.bank.poalim.inventory_service.model.Product;

@ExtendWith(MockitoExtension.class)
class InventoryValidationServiceTest {

    @Mock
    private ProductCatalogService productCatalogService;
    
    @Mock
    private OrderEventsProducer orderEventProducer;

    private InventoryValidationService inventoryValidationService;

    @BeforeEach
    void setUp() {
        inventoryValidationService = new InventoryValidationService(productCatalogService, orderEventProducer);
    }

    @Test
    void validateOrder_AllProductsAvailable_OrderApproved() {
        // Given
        String orderId = "ORDER-001";
        List<OrderItemDto> items = Arrays.asList(
                createOrderItem("P1001", 2, OrderItemCategory.STANDARD),
                createOrderItem("P3001", 1, OrderItemCategory.DIGITAL)
        );

        when(productCatalogService.findProduct("P1001"))
                .thenReturn(Optional.of(createProduct("P1001", "Standard Product", OrderItemCategory.STANDARD, 50, null, true)));
        when(productCatalogService.findProduct("P3001"))
                .thenReturn(Optional.of(createProduct("P3001", "Digital Product", OrderItemCategory.DIGITAL, 1000, null, true)));

        // When
        InventoryCheckResult result = inventoryValidationService.validateOrder(orderId, items);

        // Then
        assertThat(result.isApproved()).isTrue();
        assertThat(result.getIssues()).isEmpty();
        assertThat(result.getValidatedItems()).hasSize(2);
        assertThat(result.getValidatedItems().get(0).isAvailable()).isTrue();
        assertThat(result.getValidatedItems().get(1).isAvailable()).isTrue();
    }

    @Test
    void validateOrder_SomeProductsUnavailable_OrderRejected() {
        // Given
        String orderId = "ORDER-002";
        List<OrderItemDto> items = Arrays.asList(
                createOrderItem("P1001", 2, OrderItemCategory.STANDARD),
                createOrderItem("P1002", 15, OrderItemCategory.STANDARD) // Requesting more than available
        );

        when(productCatalogService.findProduct("P1001"))
                .thenReturn(Optional.of(createProduct("P1001", "Standard Product", OrderItemCategory.STANDARD, 50, null, true)));
        when(productCatalogService.findProduct("P1002"))
                .thenReturn(Optional.of(createProduct("P1002", "Standard Product 2", OrderItemCategory.STANDARD, 10, null, true)));

        // When
        InventoryCheckResult result = inventoryValidationService.validateOrder(orderId, items);

        // Then
        assertThat(result.isApproved()).isFalse();
        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getProductId()).isEqualTo("P1002");
        assertThat(result.getIssues().get(0).getType()).isEqualTo(InventoryCheckResult.ValidationIssueType.INSUFFICIENT_QUANTITY);
    }

    @Test
    void validateOrder_PerishableItemExpired_OrderRejected() {
        // Given
        String orderId = "ORDER-003";
        List<OrderItemDto> items = Arrays.asList(
                createOrderItem("P2002", 2, OrderItemCategory.PERISHABLE) // Expired yogurt
        );

        when(productCatalogService.findProduct("P2002"))
                .thenReturn(Optional.of(createProduct("P2002", "Expired Yogurt", OrderItemCategory.PERISHABLE, 5, LocalDate.now().minusDays(1), true)));

        // When
        InventoryCheckResult result = inventoryValidationService.validateOrder(orderId, items);

        // Then
        assertThat(result.isApproved()).isFalse();
        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getProductId()).isEqualTo("P2002");
        assertThat(result.getIssues().get(0).getType()).isEqualTo(InventoryCheckResult.ValidationIssueType.EXPIRED_PRODUCT);
    }

    @Test
    void validateOrder_ProductNotFound_OrderRejected() {
        // Given
        String orderId = "ORDER-004";
        List<OrderItemDto> items = Arrays.asList(
                createOrderItem("UNKNOWN", 1, OrderItemCategory.STANDARD)
        );

        when(productCatalogService.findProduct("UNKNOWN"))
                .thenReturn(Optional.empty());

        // When
        InventoryCheckResult result = inventoryValidationService.validateOrder(orderId, items);

        // Then
        assertThat(result.isApproved()).isFalse();
        assertThat(result.getIssues()).hasSize(1);
        assertThat(result.getIssues().get(0).getProductId()).isEqualTo("UNKNOWN");
        assertThat(result.getIssues().get(0).getType()).isEqualTo(InventoryCheckResult.ValidationIssueType.PRODUCT_NOT_FOUND);
    }

    @Test
    void validateOrder_PerishableItemNotExpired_OrderApproved() {
        // Given
        String orderId = "ORDER-005";
        List<OrderItemDto> items = Arrays.asList(
                createOrderItem("P2001", 5, OrderItemCategory.PERISHABLE)
        );

        when(productCatalogService.findProduct("P2001"))
                .thenReturn(Optional.of(createProduct("P2001", "Fresh Milk", OrderItemCategory.PERISHABLE, 20, LocalDate.now().plusDays(7), true)));

        // When
        InventoryCheckResult result = inventoryValidationService.validateOrder(orderId, items);

        // Then
        assertThat(result.isApproved()).isTrue();
        assertThat(result.getIssues()).isEmpty();
        assertThat(result.getValidatedItems()).hasSize(1);
        assertThat(result.getValidatedItems().get(0).isAvailable()).isTrue();
    }

    @Test
    void validateOrder_DigitalProduct_AlwaysAvailable() {
        // Given
        String orderId = "ORDER-006";
        List<OrderItemDto> items = Arrays.asList(
                createOrderItem("P3001", 999, OrderItemCategory.DIGITAL)
        );

        when(productCatalogService.findProduct("P3001"))
                .thenReturn(Optional.of(createProduct("P3001", "Digital Book", OrderItemCategory.DIGITAL, 1000, null, true)));

        // When
        InventoryCheckResult result = inventoryValidationService.validateOrder(orderId, items);

        // Then
        assertThat(result.isApproved()).isTrue();
        assertThat(result.getIssues()).isEmpty();
        assertThat(result.getValidatedItems()).hasSize(1);
        assertThat(result.getValidatedItems().get(0).isAvailable()).isTrue();
    }

    @Test
    void updateInventoryForApprovedOrder_UpdatesQuantities() {
        // Given
        String orderId = "ORDER-007";
        List<InventoryCheckResult.ValidatedItem> validatedItems = Arrays.asList(
        		InventoryCheckResult.ValidatedItem.builder()
                        .productId("P1001")
                        .requestedQuantity(5)
                        .availableQuantity(50)
                        .category(OrderItemCategory.STANDARD)
                        .available(true)
                        .build(),
                        InventoryCheckResult.ValidatedItem.builder()
                        .productId("P3001")
                        .requestedQuantity(10)
                        .availableQuantity(1000)
                        .category(OrderItemCategory.DIGITAL)
                        .available(true)
                        .build()
        );

        InventoryCheckResult validationResult = InventoryCheckResult.builder()
                .orderId(orderId)
                .approved(true)
                .validatedItems(validatedItems)
                .build();

        // When
        inventoryValidationService.updateInventoryForApprovedOrder(validationResult);

        // Then
        verify(productCatalogService).updateProductQuantity("P1001", 45); // 50 - 5
        // TODO fix 
//        verify(productCatalogService, never()).updateProductQuantity("P3001", anyInt()); // Digital products don't decrement
    }

    @Test
    void updateInventoryForRejectedOrder_DoesNotUpdateQuantities() {
        // Given
        String orderId = "ORDER-008";
        InventoryCheckResult validationResult = InventoryCheckResult.builder()
                .orderId(orderId)
                .approved(false)
                .build();

        // When
        inventoryValidationService.updateInventoryForApprovedOrder(validationResult);

        // Then
        verify(productCatalogService, never()).updateProductQuantity(anyString(), anyInt());
    }

    private OrderItemDto createOrderItem(String productId, int quantity, OrderItemCategory category) {
        OrderItemDto item = new OrderItemDto(
        	productId,
        	quantity,
        	category
        );
        return item;
    }

    private Product createProduct(String productId, String name, OrderItemCategory category, int quantity, LocalDate expirationDate, boolean active) {
        return Product.builder()
                .productId(productId)
                .name(name)
                .category(category)
                .availableQuantity(quantity)
                .expirationDate(expirationDate)
                .active(active)
                .build();
    }
}
