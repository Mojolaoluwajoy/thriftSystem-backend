package com.thrift.thriftsystem.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AppConfig {

    @Value("${SUPPORT_WHATSAPP}")
    private String supportWhatsapp;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private String jwtExpiration;

    public String generateWhatsappLink(String phoneNumber, String message) {
        return "https://wa.me/" + phoneNumber + "?text=" + message.replace(" ", "%20");
    }

    public String getSupportWhatsappLink(String phoneNumber) {
        return generateWhatsappLink(
                supportWhatsapp, "Hello ,i need help with the thrift system app");
    }
}
