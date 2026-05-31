package com.demo.productservice.controller;

import com.demo.productservice.dto.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/products")
public class ProductController {

    private static final List<Product> PRODUCTS = List.of(
            Product.builder()
                    .id(1L)
                    .name("Laptop Pro X1")
                    .description("High-performance laptop with 16GB RAM and 512GB SSD")
                    .price(new BigDecimal("1299.99"))
                    .category("Electronics")
                    .stockQuantity(50)
                    .build(),
            Product.builder()
                    .id(2L)
                    .name("Wireless Mouse")
                    .description("Ergonomic wireless mouse with 2.4GHz connectivity")
                    .price(new BigDecimal("29.99"))
                    .category("Accessories")
                    .stockQuantity(200)
                    .build(),
            Product.builder()
                    .id(3L)
                    .name("Mechanical Keyboard")
                    .description("TKL mechanical keyboard with Cherry MX Blue switches")
                    .price(new BigDecimal("89.99"))
                    .category("Accessories")
                    .stockQuantity(120)
                    .build(),
            Product.builder()
                    .id(4L)
                    .name("4K Monitor")
                    .description("27-inch 4K UHD IPS display with USB-C connectivity")
                    .price(new BigDecimal("549.99"))
                    .category("Electronics")
                    .stockQuantity(30)
                    .build(),
            Product.builder()
                    .id(5L)
                    .name("USB-C Hub")
                    .description("7-in-1 USB-C hub with HDMI, USB 3.0, and SD card reader")
                    .price(new BigDecimal("49.99"))
                    .category("Accessories")
                    .stockQuantity(150)
                    .build()
    );

    private static final Map<Long, Product> PRODUCT_MAP = PRODUCTS.stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Fetching all products, count={}", PRODUCTS.size());
        return ResponseEntity.ok(PRODUCTS);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        log.info("Fetching product with id={}", id);
        Product product = PRODUCT_MAP.get(id);
        if (product == null) {
            log.warn("Product not found with id={}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }
}
