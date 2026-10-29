package com.ecomm.order_service.service;

import com.ecomm.order_service.client.ProductClient;
import com.ecomm.order_service.client.dto.ProductClientResponse;
import com.ecomm.order_service.dto.OrderRequest;
import com.ecomm.order_service.dto.OrderResponse;
import com.ecomm.order_service.entity.Order;
import com.ecomm.order_service.entity.OrderStatus;
import com.ecomm.order_service.event.OrderCreatedEvent;
import com.ecomm.order_service.event.producer.OrderProducer;
import com.ecomm.order_service.exception.OrderCancellationNotAllowedException;
import com.ecomm.order_service.exception.OrderNotFoundException;
import com.ecomm.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        BigDecimal totalAmount =
                product.price()
                        .multiply(BigDecimal.valueOf(request.quantity()));

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .productId(product.id())
                .quantity(request.quantity())
                .customerEmail(request.customerEmail())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        productClient.reserve(product.id(), order.getId(),
                new ProductClient.ReservationRequest(request.quantity()));

        try {
            Order savedOrder = orderRepository.save(order);

            OrderCreatedEvent event = new OrderCreatedEvent(
                    UUID.randomUUID(),
                    savedOrder.getId(),
                    product.id(),
                    product.name(),
                    savedOrder.getCustomerEmail(),
                    savedOrder.getTotalAmount(),
                    Instant.now()
            );

            orderProducer.publishOrderCreated(event);

            return mapToResponse(savedOrder);
        } catch (RuntimeException failure) {
            try {
                productClient.release(product.id(), order.getId());
            } catch (RuntimeException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
            throw failure;
        }
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

    @Transactional
    public void cancelOrder(UUID id) {

        Order order = orderRepository.lockById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderCancellationNotAllowedException("Only pending orders can be cancelled");
        }

        productClient.release(order.getProductId(), order.getId());

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
