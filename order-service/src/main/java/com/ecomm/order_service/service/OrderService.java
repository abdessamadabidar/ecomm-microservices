package com.ecomm.order_service.service;

import com.ecomm.order_service.client.ProductClient;
import com.ecomm.order_service.client.dto.ProductClientResponse;
import com.ecomm.order_service.dto.OrderRequest;
import com.ecomm.order_service.dto.OrderResponse;
import com.ecomm.order_service.entity.Order;
import com.ecomm.order_service.entity.OrderStatus;
import com.ecomm.order_service.event.OrderCreatedEvent;
import com.ecomm.order_service.event.producer.OrderProducer;
import com.ecomm.order_service.exception.InsufficientStockException;
import com.ecomm.order_service.exception.OrderNotFoundException;
import com.ecomm.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final OrderProducer orderProducer;

    public OrderResponse createOrder(OrderRequest request) {

        ProductClientResponse product =
                productClient.getProduct(request.productId());

        if (product.quantity() < request.quantity()) {
            throw new InsufficientStockException(
                    "Not enough stock for product: " + product.id()
            );
        }

        BigDecimal totalAmount =
                product.price()
                        .multiply(BigDecimal.valueOf(request.quantity()));

        Order order = Order.builder()
                .productId(product.id())
                .quantity(request.quantity())
                .customerEmail(request.customerEmail())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        Order savedOrder = orderRepository.save(order);

        /* Create & send CREATED Order event */
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                product.id(),
                product.name(),
                order.getCustomerEmail(),
                order.getTotalAmount(),
                Instant.now()
        );

        orderProducer.publishOrderCreated(event);

        return mapToResponse(savedOrder);
    }

    public List<OrderResponse> findAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public OrderResponse findById(UUID id) {
        return mapToResponse(findOrderById(id));
    }

    public void cancelOrder(UUID id) {

        Order order = findOrderById(id);

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
    }

    private Order findOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found with id: " + id
                        )
                );
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getCustomerEmail(),
                order.getStatus()
        );
    }
}
