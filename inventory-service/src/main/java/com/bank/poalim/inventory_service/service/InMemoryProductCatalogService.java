package com.bank.poalim.inventory_service.service;

import com.bank.poalim.inventory_service.model.OrderItemCategory;
import com.bank.poalim.inventory_service.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InMemoryProductCatalogService implements ProductCatalogService {
    
    private final Map<String, Product> productCatalog = new ConcurrentHashMap<>();
    
    public InMemoryProductCatalogService() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        // Standard products
        addProduct(Product.builder()
                .productId("P1001")
                .name("Standard Product 1")
                .category(OrderItemCategory.STANDARD)
                .availableQuantity(50)
                .active(true)
                .build());
        
        addProduct(Product.builder()
                .productId("P1002")
                .name("Standard Product 2")
                .category(OrderItemCategory.STANDARD)
                .availableQuantity(10)
                .active(true)
                .build());
        
        // Perishable products
        addProduct(Product.builder()
                .productId("P2001")
                .name("Fresh Milk")
                .category(OrderItemCategory.PERISHABLE)
                .availableQuantity(20)
                .expirationDate(LocalDate.now().plusDays(7))
                .active(true)
                .build());
        
        addProduct(Product.builder()
                .productId("P2002")
                .name("Expired Yogurt")
                .category(OrderItemCategory.PERISHABLE)
                .availableQuantity(5)
                .expirationDate(LocalDate.now().minusDays(1)) // Expired
                .active(true)
                .build());
        
        addProduct(Product.builder()
                .productId("P2003")
                .name("Fresh Bread")
                .category(OrderItemCategory.PERISHABLE)
                .availableQuantity(15)
                .expirationDate(LocalDate.now().plusDays(3))
                .active(true)
                .build());
        
        // Digital products
        addProduct(Product.builder()
                .productId("P3001")
                .name("Digital Book")
                .category(OrderItemCategory.DIGITAL)
                .availableQuantity(1000) // Unlimited
                .active(true)
                .build());
        
        addProduct(Product.builder()
                .productId("P3002")
                .name("Software License")
                .category(OrderItemCategory.DIGITAL)
                .availableQuantity(500)
                .active(true)
                .build());
        
        log.info("Initialized product catalog with {} products", productCatalog.size());
    }
    
    @Override
    public Optional<Product> findProduct(String productId) {
        Product product = productCatalog.get(productId);
        return Optional.ofNullable(product);
    }
    
    @Override
    public List<Product> getAllProducts() {
        return new ArrayList<>(productCatalog.values());
    }
    
    @Override
    public void updateProductQuantity(String productId, int newQuantity) {
        Product product = productCatalog.get(productId);
        if (product != null) {
            product.setAvailableQuantity(newQuantity);
            log.info("Updated product {} quantity to {}", productId, newQuantity);
        } else {
            log.warn("Attempted to update quantity for non-existent product: {}", productId);
        }
    }
    
    @Override
    public void addProduct(Product product) {
        productCatalog.put(product.getProductId(), product);
        log.info("Added product to catalog: {}", product.getProductId());
    }
    
    @Override
    public void removeProduct(String productId) {
        Product removed = productCatalog.remove(productId);
        if (removed != null) {
            log.info("Removed product from catalog: {}", productId);
        } else {
            log.warn("Attempted to remove non-existent product: {}", productId);
        }
    }
}
