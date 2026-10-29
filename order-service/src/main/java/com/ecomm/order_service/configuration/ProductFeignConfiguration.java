package com.ecomm.order_service.configuration;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ProductFeignConfiguration {

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
                200, 1000, 3
        );
    }
}
