package com.ecomm.order_service.exception;

public class PaymentRequestNotAllowedException extends RuntimeException {
    public PaymentRequestNotAllowedException(String message) {
        super(message);
    }
}
