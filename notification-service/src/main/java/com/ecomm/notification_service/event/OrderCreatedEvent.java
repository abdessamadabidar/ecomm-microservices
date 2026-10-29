package com.ecomm.notification_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID productId,
        String productName,
        String customerEmail,
        BigDecimal totalAmount,
        Instant occurredAt
) {}
