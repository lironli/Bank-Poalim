package com.bank.poalim.inventory_service.controller;

import com.bank.poalim.inventory_service.model.Product;
import com.bank.poalim.inventory_service.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productCatalogService.getAllProducts();
        log.info("Retrieved {} products from catalog", products.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        Optional<Product> product = productCatalogService.findProduct(productId);
        if (product.isPresent()) {
            log.info("Retrieved product: {}", productId);
            return ResponseEntity.ok(product.get());
        } else {
            log.warn("Product not found: {}", productId);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Product> addProduct(@RequestBody Product product) {
        productCatalogService.addProduct(product);
        log.info("Added new product: {}", product.getProductId());
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{productId}/quantity")
    public ResponseEntity<Void> updateProductQuantity(
            @PathVariable String productId,
            @RequestParam int quantity) {
        productCatalogService.updateProductQuantity(productId, quantity);
        log.info("Updated product {} quantity to {}", productId, quantity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeProduct(@PathVariable String productId) {
        productCatalogService.removeProduct(productId);
        log.info("Removed product: {}", productId);
        return ResponseEntity.ok().build();
    }
}
