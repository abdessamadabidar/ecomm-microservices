package com.ecomm.payment_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(

        UUID eventId,

        UUID paymentId,

        UUID orderId,

        BigDecimal amount,

        String transactionId,

        Instant occurredAt

) {
}