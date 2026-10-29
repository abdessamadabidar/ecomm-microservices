package com.ecomm.product_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationRequest(@NotNull @Positive Integer quantity) {
}
