package com.ecomm.order_service.client;

import com.ecomm.order_service.exception.ProductNotFoundException;
import com.ecomm.order_service.exception.ProductServiceUnavailableException;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            if (cause instanceof FeignException.NotFound) {
                throw new ProductNotFoundException(
                      "Product not found with id: " + productId
                );
            }

            throw new ProductServiceUnavailableException(
                    "Product service is currently unavailable",
                    cause
            );
        };
    }
}
