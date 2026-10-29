package com.ecomm.order_service.entity;

public enum OrderStatus {
    PENDING_PAYMENT,

    PAYMENT_PROCESSING,

    CONFIRMED,

    PAYMENT_FAILED,

    CANCELLED
}
