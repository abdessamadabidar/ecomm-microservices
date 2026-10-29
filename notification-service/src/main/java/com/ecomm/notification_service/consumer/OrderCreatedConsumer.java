package com.ecomm.notification_service.consumer;

import com.ecomm.notification_service.event.OrderCreatedEvent;
import com.ecomm.notification_service.event.PaymentCompletedEvent;
import com.ecomm.notification_service.event.PaymentFailedEvent;
import com.ecomm.notification_service.observation.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final EventStore eventStore;

    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String PAYMENT_COMPLETED_TOPIC = "payment-completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";


    @KafkaListener(
            topics = ORDER_CREATED_TOPIC,
            groupId = "notification-service"
    )
    public void orderCreated(OrderCreatedEvent event) {

        eventStore.record(ORDER_CREATED_TOPIC, "order-service", event.orderId(), event);

        log.info(
                "Received OrderCreatedEvent: orderId={}, email={}, amount={}",
                event.orderId(),
                event.customerEmail(),
                event.totalAmount()
        );

        sendNotification(event);
    }

    @KafkaListener(
            topics = PAYMENT_COMPLETED_TOPIC,
            groupId = "notification-service"
    )
    public void paymentCompleted(
            PaymentCompletedEvent event
    ) {

        eventStore.record(PAYMENT_COMPLETED_TOPIC, "payment-service", event.orderId(), event);

        log.info(
                "Payment successful notification for order {}",
                event.orderId()
        );
    }

    @KafkaListener(
            topics = PAYMENT_FAILED_TOPIC,
            groupId = "notification-service"
    )
    public void paymentFailed(
            PaymentFailedEvent event
    ) {

        eventStore.record(PAYMENT_FAILED_TOPIC, "payment-service", event.orderId(), event);

        log.info(
                "Payment failed notification for order {}: {}",
                event.orderId(),
                event.reason()
        );
    }

    private void sendNotification(OrderCreatedEvent event) {

        log.info(
                "Sending order confirmation notification to {} for order {}",
                event.customerEmail(),
                event.orderId()
        );

        // Simulated notification for now.
    }
}
