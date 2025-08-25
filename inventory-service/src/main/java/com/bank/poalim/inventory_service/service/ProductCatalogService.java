package com.bank.poalim.inventory_service.service;

import com.bank.poalim.inventory_service.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductCatalogService {
    Optional<Product> findProduct(String productId);
    List<Product> getAllProducts();
    void updateProductQuantity(String productId, int newQuantity);
    void addProduct(Product product);
    void removeProduct(String productId);
}
