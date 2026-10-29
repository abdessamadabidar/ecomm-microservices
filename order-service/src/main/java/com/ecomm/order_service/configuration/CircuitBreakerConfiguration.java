package com.ecomm.order_service.configuration;

import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfiguration {

    public CircuitBreakerNameResolver circuitBreakerNameResolver() {
        return (feignClientName, target, method) ->
                "productService" + Character.toUpperCase(method.getName().charAt(0)) +
                        method.getName().substring(1);
    }
}
