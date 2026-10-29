package com.ecomm.order_service.client;

import com.ecomm.order_service.exception.ProductNotFoundException;
import com.ecomm.order_service.exception.ProductServiceUnavailableException;
import com.ecomm.order_service.exception.InsufficientStockException;
import com.ecomm.order_service.client.dto.ProductClientResponse;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductClientResponse getProduct(UUID productId) {
                throw translated(cause, productId);
            }

            @Override
            public void reserve(UUID productId, UUID orderId, ReservationRequest request) {
                throw translated(cause, productId);
            }

            @Override
            public void release(UUID productId, UUID orderId) {
                throw translated(cause, productId);
            }
        };
    }

    private RuntimeException translated(Throwable cause, UUID productId) {
        if (cause instanceof FeignException.NotFound) {
            return new ProductNotFoundException("Product not found with id: " + productId);
        }
        if (cause instanceof FeignException.Conflict) {
            return new InsufficientStockException("Not enough stock for product: " + productId);
        }
        return new ProductServiceUnavailableException("Product service is currently unavailable", cause);
    }
}
