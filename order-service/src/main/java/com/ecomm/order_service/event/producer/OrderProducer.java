package com.ecomm.order_service.event.producer;

import com.ecomm.order_service.event.OrderCreatedEvent;
import com.ecomm.order_service.event.PaymentRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderProducer {

    private static final String TOPIC = "order-created";
    private static final String PAYMENT_REQUESTED_TOPIC = "payment-requested";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(
                TOPIC,
                event.orderId().toString(),
                event
        );
    }

    public void publishPaymentRequested(PaymentRequestedEvent event) {
        kafkaTemplate.send(PAYMENT_REQUESTED_TOPIC, event.orderId().toString(), event);
    }
}
