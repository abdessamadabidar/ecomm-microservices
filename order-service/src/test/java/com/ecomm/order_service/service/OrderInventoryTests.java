package com.ecomm.order_service.service;

import com.ecomm.order_service.client.ProductClient;
import com.ecomm.order_service.client.dto.ProductClientResponse;
import com.ecomm.order_service.dto.OrderRequest;
import com.ecomm.order_service.entity.Order;
import com.ecomm.order_service.entity.OrderStatus;
import com.ecomm.order_service.event.producer.OrderProducer;
import com.ecomm.order_service.exception.InsufficientStockException;
import com.ecomm.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderInventoryTests {
    @Mock OrderRepository orders;
    @Mock ProductClient products;
    @Mock OrderProducer producer;
    @InjectMocks OrderService service;

    @Test
    void newOrderReservesStockBeforePublishing() {
        UUID productId = UUID.randomUUID();
        when(products.getProduct(productId)).thenReturn(new ProductClientResponse(productId, "Desk", "", new BigDecimal("10.00"), 5));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createOrder(new OrderRequest(productId, 2, "demo@example.com"));

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        var sequence = inOrder(products, orders, producer);
        sequence.verify(products).reserve(eq(productId), eq(result.id()), any(ProductClient.ReservationRequest.class));
        sequence.verify(orders).save(any(Order.class));
        sequence.verify(producer).publishOrderCreated(any());
    }

    @Test
    void rejectedReservationDoesNotSaveOrPublishOrder() {
        UUID productId = UUID.randomUUID();
        when(products.getProduct(productId)).thenReturn(new ProductClientResponse(productId, "Desk", "", new BigDecimal("10.00"), 5));
        doThrow(new InsufficientStockException("Not enough stock"))
                .when(products).reserve(eq(productId), any(UUID.class), any(ProductClient.ReservationRequest.class));

        assertThatThrownBy(() -> service.createOrder(new OrderRequest(productId, 2, "demo@example.com")))
                .isInstanceOf(InsufficientStockException.class);
        verifyNoInteractions(orders, producer);
    }

    @Test
    void cancellingPendingOrderReturnsItsReservationOnce() {
        UUID productId = UUID.randomUUID(), orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).productId(productId).quantity(2)
                .status(OrderStatus.PENDING_PAYMENT).build();
        when(orders.lockById(orderId)).thenReturn(Optional.of(order));
        when(orders.save(order)).thenReturn(order);

        service.cancelOrder(orderId);
        service.cancelOrder(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(products, times(1)).release(productId, orderId);
    }
}
