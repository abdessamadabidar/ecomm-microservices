package com.ecomm.order_service.dto;

import com.ecomm.order_service.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID productId,
        Integer quantity,
        BigDecimal totalAmount,
        String customerEmail,
        OrderStatus status
) {
}