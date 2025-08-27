package com.bank.poalim.inventory_service.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bank.poalim.inventory_service.event.InventoryCheckResultEvent;
import com.bank.poalim.inventory_service.kafka.OrderEventsProducer;
import com.bank.poalim.inventory_service.model.OrderItemCategory;
import com.bank.poalim.inventory_service.model.OrderItemDto;
import com.bank.poalim.inventory_service.model.InventoryCheckResult;
import com.bank.poalim.inventory_service.model.Product;
import com.bank.poalim.inventory_service.model.ValidationMissingItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryValidationService {
    
    private final ProductCatalogService productCatalogService;
    private final OrderEventsProducer orderEventProducer;
    
    public InventoryCheckResult validateOrder(String orderId, List<OrderItemDto> items) {
        log.info("Validating order {} with {} items", orderId, items.size());
        
        List<InventoryCheckResult.ValidationIssue> issues = new ArrayList<>();
        List<InventoryCheckResult.ValidatedItem> validatedItems = new ArrayList<>();
        boolean orderApproved = true;
        
        for (OrderItemDto item : items) {
            InventoryCheckResult.ValidatedItem validatedItem = validateItem(item);
            validatedItems.add(validatedItem);
            
            if (!validatedItem.isAvailable()) {
                orderApproved = false;
                // Add issue for unavailable items
                issues.add(InventoryCheckResult.ValidationIssue.builder()
                        .productId(item.getProductId())
                        .reason(getIssueReason(validatedItem))
                        .type(getIssueType(validatedItem))
                        .build());
            }
        }
        
        InventoryCheckResult result = InventoryCheckResult.builder()
                .orderId(orderId)
                .approved(orderApproved)
                .issues(issues)
                .validatedItems(validatedItems)
                .build();
        
        if (orderApproved) {
            log.info("Order {} APPROVED - all items available", orderId);
            // Update inventory
            updateInventoryForApprovedOrder(result);
        } else {
            log.warn("Order {} REJECTED - {} issues found", orderId, issues.size());
            for (InventoryCheckResult.ValidationIssue issue : issues) {
                log.warn("Issue: Product {} - {}", issue.getProductId(), issue.getReason());
            }
        }
        
        publishInvenoryCheckResultEvent(result);
        
        return result;
    }
    
    private InventoryCheckResult.ValidatedItem validateItem(OrderItemDto item) {
        return productCatalogService.findProduct(item.getProductId())
                .map(product -> validateProductAvailability(product, item))
                .orElse(createNotFoundValidatedItem(item));
    }
    
    private InventoryCheckResult.ValidatedItem validateProductAvailability(Product product, OrderItemDto item) {
        boolean available = false;
        
        if (!product.isActive()) {
            return InventoryCheckResult.ValidatedItem.builder()
                    .productId(item.getProductId())
                    .requestedQuantity(item.getQuantity())
                    .availableQuantity(0)
                    .category(product.getCategory())
                    .available(false)
                    .build();
        }
        
        switch (product.getCategory()) {
            case STANDARD:
                available = product.getAvailableQuantity() >= item.getQuantity();
                break;
                
            case PERISHABLE:
                boolean notExpired = product.getExpirationDate() != null && 
                                   product.getExpirationDate().isAfter(LocalDate.now());
                boolean sufficientQuantity = product.getAvailableQuantity() >= item.getQuantity();
                available = notExpired && sufficientQuantity;
                break;
                
            case DIGITAL:
                available = true; // Digital products are always available
                break;
                
            default:
                available = false;
                break;
        }
        
        return InventoryCheckResult.ValidatedItem.builder()
                .productId(item.getProductId())
                .requestedQuantity(item.getQuantity())
                .availableQuantity(product.getAvailableQuantity())
                .category(product.getCategory())
                .available(available)
                .build();
    }
    
    private InventoryCheckResult.ValidatedItem createNotFoundValidatedItem(OrderItemDto item) {
        return InventoryCheckResult.ValidatedItem.builder()
                .productId(item.getProductId())
                .requestedQuantity(item.getQuantity())
                .availableQuantity(0)
                .category(null)
                .available(false)
                .build();
    }
    
    private String getIssueReason(InventoryCheckResult.ValidatedItem validatedItem) {
        if (validatedItem.getCategory() == null) {
            return "Product not found in catalog";
        }
        
        switch (validatedItem.getCategory()) {
            case STANDARD:
                return String.format("Insufficient quantity. Requested: %d, Available: %d", 
                        validatedItem.getRequestedQuantity(), validatedItem.getAvailableQuantity());
            case PERISHABLE:
                return "Product expired or insufficient quantity";
            case DIGITAL:
                return "Digital product unavailable (should not happen)";
            default:
                return "Invalid category";
        }
    }
    
    private InventoryCheckResult.ValidationIssueType getIssueType(InventoryCheckResult.ValidatedItem validatedItem) {
        if (validatedItem.getCategory() == null) {
            return InventoryCheckResult.ValidationIssueType.PRODUCT_NOT_FOUND;
        }
        
        switch (validatedItem.getCategory()) {
            case STANDARD:
                return InventoryCheckResult.ValidationIssueType.INSUFFICIENT_QUANTITY;
            case PERISHABLE:
                return InventoryCheckResult.ValidationIssueType.EXPIRED_PRODUCT;
            case DIGITAL:
                return InventoryCheckResult.ValidationIssueType.PRODUCT_INACTIVE;
            default:
                return InventoryCheckResult.ValidationIssueType.INVALID_CATEGORY;
        }
    }
    
    public void updateInventoryForApprovedOrder(InventoryCheckResult validationResult) {
        if (!validationResult.isApproved()) {
            log.warn("Attempted to update inventory for rejected order: {}", validationResult.getOrderId());
            return;
        }
        
        log.info("Updating inventory for approved order: {}", validationResult.getOrderId());
        
        for (InventoryCheckResult.ValidatedItem item : validationResult.getValidatedItems()) {
            if (item.isAvailable() && item.getCategory() != OrderItemCategory.DIGITAL) {
                // For digital products, we don't decrement inventory
                int newQuantity = item.getAvailableQuantity() - item.getRequestedQuantity();
                productCatalogService.updateProductQuantity(item.getProductId(), newQuantity);
                log.info("Updated inventory for product {}: {} -> {}", 
                        item.getProductId(), item.getAvailableQuantity(), newQuantity);
            }
        }
    }
    
    public void publishInvenoryCheckResultEvent(InventoryCheckResult result) {
    	
    	try {
    		
    		List<ValidationMissingItem> validationMissingItems = new ArrayList<ValidationMissingItem>();
    		if(!result.isApproved()) {
    			result.getIssues().forEach(i ->
        		validationMissingItems.add(new ValidationMissingItem(i.getProductId(), i.getReason()))
    					);
    		}
        	
        	
        	
        	InventoryCheckResultEvent event = InventoryCheckResultEvent.builder()
                    .orderId(result.getOrderId())
                    .missingItems(validationMissingItems.isEmpty() ? null : validationMissingItems)
                    .approved(result.isApproved()? true : false)
                    .build();
        	
        	orderEventProducer.publishInventoryCheckResultEvent(event);
            log.info("Inventory check result event published to Kafka for order ID: {}", result.getOrderId());
        	
    	} catch (Exception e) {
            log.error("Failed to publish inventory check result event to Kafka for order ID: {}", result.getOrderId(), e);
            // Ideally we should have a retry mechanism to publish the event again
        }
    	
    	
    	
    	
    }
    
 
   
}
