package com.ecomm.order_service.event.consumer;


import com.ecomm.order_service.entity.Order;
import com.ecomm.order_service.entity.OrderStatus;
import com.ecomm.order_service.event.PaymentCompletedEvent;
import com.ecomm.order_service.event.PaymentFailedEvent;
import com.ecomm.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;

    @Transactional
    @KafkaListener(
            topics = "payment-completed",
            groupId = "order-service"
    )
    public void paymentCompleted(
            PaymentCompletedEvent event
    ) {

        Order order = orderRepository
                .findById(event.orderId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Order not found: "
                                        + event.orderId()
                        )
                );

        if (order.getStatus() ==
                OrderStatus.CONFIRMED) {

            log.warn(
                    "Order {} already confirmed",
                    order.getId()
            );

            return;
        }

        order.setStatus(
                OrderStatus.CONFIRMED
        );

        orderRepository.save(order);

        log.info(
                "Order {} confirmed after payment {}",
                order.getId(),
                event.paymentId()
        );
    }

    @Transactional
    @KafkaListener(
            topics = "payment-failed",
            groupId = "order-service"
    )
    public void paymentFailed(
            PaymentFailedEvent event
    ) {

        Order order = orderRepository
                .findById(event.orderId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Order not found: "
                                        + event.orderId()
                        )
                );

        if (order.getStatus() ==
                OrderStatus.PAYMENT_FAILED) {

            return;
        }

        order.setStatus(
                OrderStatus.PAYMENT_FAILED
        );

        orderRepository.save(order);

        log.warn(
                "Payment failed for order {}: {}",
                order.getId(),
                event.reason()
        );
    }
}
