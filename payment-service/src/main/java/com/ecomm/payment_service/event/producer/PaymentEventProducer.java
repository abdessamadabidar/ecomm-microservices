package com.ecomm.payment_service.event.producer;


import com.ecomm.payment_service.event.PaymentCompletedEvent;
import com.ecomm.payment_service.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String PAYMENT_COMPLETED_TOPIC =
            "payment-completed";

    private static final String PAYMENT_FAILED_TOPIC =
            "payment-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCompleted(
            PaymentCompletedEvent event
    ) {

        kafkaTemplate.send(
                PAYMENT_COMPLETED_TOPIC,
                event.orderId().toString(),
                event
        );
    }

    public void publishFailed(
            PaymentFailedEvent event
    ) {

        kafkaTemplate.send(
                PAYMENT_FAILED_TOPIC,
                event.orderId().toString(),
                event
        );
    }
}