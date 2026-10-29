package com.ecomm.order_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderRequest(

        @NotNull
        UUID productId,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        @Email
        String customerEmail

) {
}