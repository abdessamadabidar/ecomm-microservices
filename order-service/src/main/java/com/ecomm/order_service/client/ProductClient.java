package com.ecomm.order_service.client;

import com.ecomm.order_service.client.dto.ProductClientResponse;
import com.ecomm.order_service.configuration.ProductFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "product-service",
        url = "${PRODUCT_SERVICE_URL:}",
        configuration = ProductFeignConfiguration.class,
        fallbackFactory = ProductClientFallbackFactory.class
)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductClientResponse getProduct( @PathVariable UUID id );

    @PutMapping("/api/products/{id}/reservations/{orderId}")
    void reserve(@PathVariable UUID id, @PathVariable UUID orderId,
                 @RequestBody ReservationRequest request);

    @DeleteMapping("/api/products/{id}/reservations/{orderId}")
    void release(@PathVariable UUID id, @PathVariable UUID orderId);

    record ReservationRequest(Integer quantity) {}

}
