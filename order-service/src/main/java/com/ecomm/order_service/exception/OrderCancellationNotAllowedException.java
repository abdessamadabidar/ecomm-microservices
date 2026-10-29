package com.ecomm.order_service.exception;

public class OrderCancellationNotAllowedException extends RuntimeException {
    public OrderCancellationNotAllowedException(String message) {
        super(message);
    }
}
