package com.ecomm.order_service.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductClientResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity
) {}