package com.demo.orderservice.controller;

import com.demo.orderservice.dto.CreateOrderRequest;
import com.demo.orderservice.dto.Order;
import com.demo.orderservice.dto.Order.OrderStatus;
import com.demo.orderservice.dto.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final AtomicLong ID_SEQUENCE = new AtomicLong(3);

    // Simulate an in-memory order store seeded with sample data
    private static final List<Order> ORDERS = new ArrayList<>(List.of(
            Order.builder()
                    .id(1L)
                    .customerName("Alice Johnson")
                    .customerEmail("alice@example.com")
                    .items(List.of(
                            OrderItem.builder().productId(1L).productName("Laptop Pro X1").quantity(1).unitPrice(new BigDecimal("1299.99")).build(),
                            OrderItem.builder().productId(2L).productName("Wireless Mouse").quantity(2).unitPrice(new BigDecimal("29.99")).build()
                    ))
                    .totalAmount(new BigDecimal("1359.97"))
                    .status(OrderStatus.DELIVERED)
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .build(),
            Order.builder()
                    .id(2L)
                    .customerName("Bob Smith")
                    .customerEmail("bob@example.com")
                    .items(List.of(
                            OrderItem.builder().productId(4L).productName("4K Monitor").quantity(1).unitPrice(new BigDecimal("549.99")).build(),
                            OrderItem.builder().productId(3L).productName("Mechanical Keyboard").quantity(1).unitPrice(new BigDecimal("89.99")).build(),
                            OrderItem.builder().productId(5L).productName("USB-C Hub").quantity(1).unitPrice(new BigDecimal("49.99")).build()
                    ))
                    .totalAmount(new BigDecimal("689.97"))
                    .status(OrderStatus.SHIPPED)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build()
    ));

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("Fetching all orders, count={}", ORDERS.size());
        return ResponseEntity.ok(ORDERS);
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Creating order for customer={}", request.getCustomerName());

        List<OrderItem> items = request.getItems().stream()
                .map(itemReq -> OrderItem.builder()
                        .productId(itemReq.getProductId())
                        .productName("Product-" + itemReq.getProductId())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(new BigDecimal("99.99"))
                        .build())
                .toList();

        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order newOrder = Order.builder()
                .id(ID_SEQUENCE.incrementAndGet())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .items(items)
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        ORDERS.add(newOrder);
        log.info("Order created successfully with id={}", newOrder.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);
    }
}
