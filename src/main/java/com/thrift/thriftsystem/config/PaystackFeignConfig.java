package com.thrift.thriftsystem.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaystackFeignConfig {

    @Value("${PAYSTACK_SECRET_KEY}")
    private String secretKey;

    @Bean
    public RequestInterceptor paystackRequestInterceptor() {
        return template -> {
            template.header("Authorization", "Bearer " + secretKey);
            template.header("Content-Type", "application/json");
        };
    }
}
