package com.ecomm.notification_service.observation;

import java.time.Instant;
import java.util.UUID;

public record ObservedEvent(
        UUID id,
        String topic,
        String source,
        String consumer,
        UUID orderId,
        Instant receivedAt,
        Object payload
) {}
