package com.ecomm.payment_service.event.consumer;

import com.ecomm.payment_service.event.PaymentRequestedEvent;
import com.ecomm.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestedConsumer {
    private final PaymentService paymentService;

    @KafkaListener(topics = "payment-requested", groupId = "payment-service")
    public void consume(PaymentRequestedEvent event) {
        log.info("Received payment-requested: orderId={}, amount={}", event.orderId(), event.totalAmount());
        paymentService.processPayment(event);
    }
}
