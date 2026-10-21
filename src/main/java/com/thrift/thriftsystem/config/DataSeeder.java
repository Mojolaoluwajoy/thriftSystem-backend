package com.thrift.thriftsystem.config;

import com.thrift.thriftsystem.enums.Role;
import com.thrift.thriftsystem.model.Currency;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.CurrencyRepository;
import com.thrift.thriftsystem.repository.UserRepository;
import com.thrift.thriftsystem.service.CurrencyService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CurrencyRepository currencyRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrencyService currencyService;

    @Value("${SUPER_ADMIN_EMAIL}")
    private String superAdminEmail;

    @Value("${SUPER_ADMIN_PASSWORD}")
    private String superAdminPassword;

    @Value("${SUPER_ADMIN_FIRSTNAME}")
    private String superAdminFirstName;

    @Value("${SUPER_ADMIN_LASTNAME}")
    private String superAdminLastName;

    @Override
    public void run(String... args) {
        seedSuperAdmin();
        seedCurrencies();
        currencyService.updateExchangeRates();
    }

    private void seedSuperAdmin() {
        removePlaceholderSuperAdmin();

        if (!userRepository.existsByEmail(superAdminEmail)) {
            User superAdmin =
                    User.builder()
                            .email(superAdminEmail)
                            .password(passwordEncoder.encode(superAdminPassword))
                            .firstName(superAdminFirstName)
                            .lastName(superAdminLastName)
                            .roles(Set.of(Role.ROLE_SUPER_ADMIN))
                            .enabled(true)
                            .emailVerified(true)
                            .build();
            userRepository.save(superAdmin);
            log.info("Super admin has been created successfully");
        } else {
            log.info("Super admin already exists");
        }
    }

    // Earlier versions had unclosed @Value placeholders, so a super admin was saved with the
    // literal email "${SUPER_ADMIN_EMAIL"; remove it now that the real one is created
    private void removePlaceholderSuperAdmin() {
        userRepository
                .findByEmail("${SUPER_ADMIN_EMAIL")
                .ifPresent(
                        user -> {
                            userRepository.delete(user);
                            log.info("Removed placeholder super admin account");
                        });
    }

    private void seedCurrencies() {
        List<Currency> defaultCurrencies =
                Arrays.asList(
                        Currency.builder()
                                .code("NGN")
                                .name("Nigerian Naira")
                                .symbol("#")
                                .exchangeRate(BigDecimal.ONE)
                                .active(true)
                                .build(),
                        Currency.builder()
                                .code("USD")
                                .name("US Dollar")
                                .symbol("$")
                                .exchangeRate(new BigDecimal("1500.00"))
                                .active(true)
                                .build(),
                        Currency.builder()
                                .code("EUR")
                                .name("Euro")
                                .symbol("E")
                                .exchangeRate(new BigDecimal("1900.00"))
                                .active(true)
                                .build(),
                        Currency.builder()
                                .code("CAD")
                                .name("Canadian Dollar")
                                .symbol("CA$")
                                .exchangeRate(new BigDecimal("1100.00"))
                                .active(true)
                                .build());
        defaultCurrencies.forEach(
                currency -> {
                    if (!currencyRepository.existsByCode(currency.getCode())) {
                        currencyRepository.save(currency);
                        log.info(
                                "Current currency has been created successfully;{}",
                                currency.getCode());
                    }
                });
    }
}
