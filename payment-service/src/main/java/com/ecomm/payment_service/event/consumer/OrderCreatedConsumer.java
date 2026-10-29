package com.ecomm.payment_service.event.consumer;


import com.ecomm.payment_service.event.OrderCreatedEvent;
import com.ecomm.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "order-created",
            groupId = "payment-service"
    )
    public void consume(
            OrderCreatedEvent event
    ) {

        log.info(
                "Received order-created: orderId={}, amount={}",
                event.orderId(),
                event.totalAmount()
        );

        paymentService.processPayment(event);
    }
}