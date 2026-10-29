package com.ecomm.order_service.exception;

public class ProductServiceUnavailableException extends RuntimeException {
    public ProductServiceUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

}
