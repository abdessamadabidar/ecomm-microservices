package com.ecomm.order_service.client;

import com.ecomm.order_service.client.dto.ProductClientResponse;
import com.ecomm.order_service.configuration.ProductFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "product-service",
        configuration = ProductFeignConfiguration.class,
        fallbackFactory = ProductClientFallbackFactory.class
)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductClientResponse getProduct( @PathVariable UUID id );

}
