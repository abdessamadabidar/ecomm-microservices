package com.ecomm.notification_service.observation;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/** Small, process-local view of messages actually received by this consumer. */
@Component
public class EventStore {
    private static final int MAX_EVENTS = 200;
    private final Deque<ObservedEvent> events = new ArrayDeque<>();

    public synchronized void record(String topic, String source, UUID orderId, Object payload) {
        events.addFirst(new ObservedEvent(UUID.randomUUID(), topic, source,
                "notification-service", orderId, Instant.now(), payload));
        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }

    public synchronized List<ObservedEvent> findAll(UUID orderId) {
        return events.stream()
                .filter(event -> orderId == null || orderId.equals(event.orderId()))
                .toList();
    }
}
