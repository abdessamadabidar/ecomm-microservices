package com.ecomm.notification_service.consumer;

import com.ecomm.notification_service.event.OrderCreatedEvent;
import com.ecomm.notification_service.event.PaymentCompletedEvent;
import com.ecomm.notification_service.event.PaymentFailedEvent;
import com.ecomm.notification_service.observation.EventStore;
import com.ecomm.notification_service.observation.ObservedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCreatedConsumerTests {
    @Test
    void capturesEveryConsumedEventWithItsOriginalFields() {
        EventStore store = new EventStore();
        OrderCreatedConsumer consumer = new OrderCreatedConsumer(store);
        UUID orderId = UUID.randomUUID();
        var created = new OrderCreatedEvent(UUID.randomUUID(), orderId, UUID.randomUUID(),
                "Keyboard", "demo@example.com", new BigDecimal("75.00"), Instant.now());
        var completed = new PaymentCompletedEvent(UUID.randomUUID(), UUID.randomUUID(),
                orderId, new BigDecimal("75.00"), "transaction", Instant.now());
        var failed = new PaymentFailedEvent(UUID.randomUUID(), UUID.randomUUID(),
                orderId, new BigDecimal("6000.00"), "Payment amount exceeds demo limit", Instant.now());

        consumer.orderCreated(created);
        consumer.paymentCompleted(completed);
        consumer.paymentFailed(failed);

        assertThat(store.findAll(orderId)).extracting(ObservedEvent::payload)
                .containsExactly(failed, completed, created);
        assertThat(store.findAll(orderId)).extracting(ObservedEvent::topic)
                .containsExactly("payment-failed", "payment-completed", "order-created");
    }
}
