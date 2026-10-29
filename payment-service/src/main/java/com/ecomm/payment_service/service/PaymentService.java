package com.ecomm.payment_service.service;

import com.ecomm.payment_service.PaymentRepository;
import com.ecomm.payment_service.entity.Payment;
import com.ecomm.payment_service.entity.PaymentStatus;
import com.ecomm.payment_service.event.PaymentRequestedEvent;
import com.ecomm.payment_service.event.PaymentCompletedEvent;
import com.ecomm.payment_service.event.PaymentFailedEvent;
import com.ecomm.payment_service.event.producer.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final BigDecimal PAYMENT_LIMIT =
            BigDecimal.valueOf(5000);

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;

    public void processPayment(
            PaymentRequestedEvent event
    ) {

        if (paymentRepository.existsByOrderId(
                event.orderId()
        )) {

            log.warn(
                    "Payment already exists for order {}. Ignoring duplicate event.",
                    event.orderId()
            );

            return;
        }

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .amount(event.totalAmount())
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        payment = paymentRepository.save(payment);

        if (event.totalAmount()
                .compareTo(PAYMENT_LIMIT) > 0) {

            failPayment(payment);

            return;
        }

        completePayment(payment);
    }

    private void completePayment(Payment payment) {

        String transactionId =
                UUID.randomUUID().toString();

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(transactionId);

        Payment saved =
                paymentRepository.save(payment);

        PaymentCompletedEvent event =
                new PaymentCompletedEvent(
                        UUID.randomUUID(),
                        saved.getId(),
                        saved.getOrderId(),
                        saved.getAmount(),
                        saved.getTransactionId(),
                        Instant.now()
                );

        eventProducer.publishCompleted(event);
    }

    private void failPayment(Payment payment) {

        String reason =
                "Payment amount exceeds demo limit";

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);

        Payment saved =
                paymentRepository.save(payment);

        PaymentFailedEvent event =
                new PaymentFailedEvent(
                        UUID.randomUUID(),
                        saved.getId(),
                        saved.getOrderId(),
                        saved.getAmount(),
                        reason,
                        Instant.now()
                );

        eventProducer.publishFailed(event);
    }
}
