package com.ecomm.notification_service.observation;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventStoreTests {
    @Test
    void preservesPayloadAndReturnsNewestObservationsFirst() {
        EventStore store = new EventStore();
        UUID orderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("orderId", orderId, "amount", 75);
        store.record("order-created", "order-service", orderId, payload);
        store.record("payment-completed", "payment-service", orderId, payload);

        var events = store.findAll(null);
        assertThat(events).extracting(ObservedEvent::topic)
                .containsExactly("payment-completed", "order-created");
        assertThat(events.getFirst().payload()).isEqualTo(payload);
        assertThat(events.getFirst().receivedAt()).isNotNull();
        assertThat(events.getFirst().source()).isEqualTo("payment-service");
        assertThat(events.getFirst().consumer()).isEqualTo("notification-service");
    }

    @Test
    void filtersByOrderWithoutChangingTheStore() {
        EventStore store = new EventStore();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        store.record("order-created", "order-service", first, Map.of());
        store.record("order-created", "order-service", second, Map.of());

        assertThat(store.findAll(first)).extracting(ObservedEvent::orderId).containsExactly(first);
        assertThat(store.findAll(UUID.randomUUID())).isEmpty();
        assertThat(store.findAll(null)).hasSize(2);
    }

    @Test
    void retainsOnlyTheLatestTwoHundredAndReturnsAnIndependentSnapshot() {
        EventStore store = new EventStore();
        for (int i = 0; i < 205; i++) {
            store.record("order-created", "order-service", UUID.randomUUID(), i);
        }
        var snapshot = store.findAll(null);
        assertThat(snapshot).hasSize(200);
        assertThat(snapshot.getFirst().payload()).isEqualTo(204);
        assertThat(snapshot.getLast().payload()).isEqualTo(5);
        store.record("order-created", "order-service", UUID.randomUUID(), 205);
        assertThat(snapshot.getFirst().payload()).isEqualTo(204);
    }
}
