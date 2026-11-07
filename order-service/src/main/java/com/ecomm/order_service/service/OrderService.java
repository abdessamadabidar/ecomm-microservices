package com.ecomm.order_service.service;

import com.ecomm.order_service.client.ProductClient;
import com.ecomm.order_service.client.dto.ProductClientResponse;
import com.ecomm.order_service.dto.OrderRequest;
import com.ecomm.order_service.dto.OrderResponse;
import com.ecomm.order_service.entity.Order;
import com.ecomm.order_service.entity.OrderStatus;
import com.ecomm.order_service.entity.OutboxEvent;
import com.ecomm.order_service.event.OrderCreatedEvent;
import com.ecomm.order_service.event.PaymentRequestedEvent;
import com.ecomm.order_service.event.producer.OrderProducer;
import com.ecomm.order_service.exception.OrderCancellationNotAllowedException;
import com.ecomm.order_service.exception.OrderNotFoundException;
import com.ecomm.order_service.exception.PaymentRequestNotAllowedException;
import com.ecomm.order_service.repository.OrderRepository;
import com.ecomm.order_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        ProductClientResponse product =
                productClient.getProduct(request.productId());

        BigDecimal totalAmount = product.price()
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

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            productClient.release(product.id(), order.getId());
                        }
                    }
                });

        orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                product.id(),
                product.name(),
                order.getCustomerEmail(),
                order.getTotalAmount(),
                Instant.now()
        );

        OutboxEvent outbox = new OutboxEvent();
        outbox.setAggregateType("Order");
        outbox.setAggregateId(order.getId().toString());
        outbox.setType("OrderCreated");
        outbox.setPayload(objectMapper.valueToTree(event));

        outboxEventRepository.save(outbox);

        return mapToResponse(order);
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

    @Transactional
    public OrderResponse requestPayment(UUID id) {
        Order order = orderRepository.lockById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                && order.getStatus() != OrderStatus.PAYMENT_PROCESSING) {
            throw new PaymentRequestNotAllowedException(
                    "Only pending orders can request payment");
        }

        order.setStatus(OrderStatus.PAYMENT_PROCESSING);
        orderRepository.save(order);

        PaymentRequestedEvent event = new PaymentRequestedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getTotalAmount(),
                Instant.now()
        );

        OutboxEvent outbox = new OutboxEvent();
        outbox.setAggregateType("Order");
        outbox.setAggregateId(order.getId().toString());
        outbox.setType("PaymentRequested");
        outbox.setPayload(objectMapper.valueToTree(event));

        outboxEventRepository.save(outbox);

        return mapToResponse(order);
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
