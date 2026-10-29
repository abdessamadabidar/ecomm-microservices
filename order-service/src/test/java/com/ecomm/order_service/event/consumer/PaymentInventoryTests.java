package com.ecomm.order_service.event.consumer;

import com.ecomm.order_service.client.ProductClient;
import com.ecomm.order_service.entity.Order;
import com.ecomm.order_service.entity.OrderStatus;
import com.ecomm.order_service.event.PaymentCompletedEvent;
import com.ecomm.order_service.event.PaymentFailedEvent;
import com.ecomm.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInventoryTests {
    @Mock OrderRepository orders;
    @Mock ProductClient products;
    @InjectMocks PaymentEventConsumer consumer;

    @Test
    void failedPaymentReturnsReservedStockOnce() {
        UUID productId = UUID.randomUUID(), orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).productId(productId).quantity(2)
                .status(OrderStatus.PENDING_PAYMENT).build();
        when(orders.lockById(orderId)).thenReturn(Optional.of(order));
        PaymentFailedEvent event = new PaymentFailedEvent(UUID.randomUUID(), UUID.randomUUID(),
                orderId, BigDecimal.TEN, "declined", Instant.now());

        consumer.paymentFailed(event);
        consumer.paymentFailed(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(products, times(1)).release(productId, orderId);
    }

    @Test
    void lateSuccessDoesNotReconfirmCancelledOrder() {
        UUID productId = UUID.randomUUID(), orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).productId(productId).quantity(2)
                .status(OrderStatus.CANCELLED).build();
        when(orders.lockById(orderId)).thenReturn(Optional.of(order));
        PaymentCompletedEvent event = new PaymentCompletedEvent(UUID.randomUUID(), UUID.randomUUID(),
                orderId, BigDecimal.TEN, "transaction", Instant.now());

        consumer.paymentCompleted(event);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verifyNoInteractions(products);
        verify(orders, never()).save(order);
    }
}
