package com.ecomm.payment_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRequestedEvent(
        UUID eventId,
        UUID orderId,
        BigDecimal totalAmount,
        Instant occurredAt
) {}
