package com.inghubs.brokage_service.config;

import com.inghubs.brokage_service.model.entity.Asset;
import com.inghubs.brokage_service.model.entity.Customer;
import com.inghubs.brokage_service.model.enums.UserRole;
import com.inghubs.brokage_service.repository.AssetRepository;
import com.inghubs.brokage_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            log.info("Database is empty, initializing default data");
            initializeData();
        }
    }
    
    private void initializeData() {
        Customer admin = Customer.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@brokage.com")
                .role(UserRole.ADMIN)
                .build();
        admin = customerRepository.save(admin);
        
        Customer customer1 = Customer.builder()
                .username("customer1")
                .password(passwordEncoder.encode("customer123"))
                .email("customer1@example.com")
                .role(UserRole.CUSTOMER)
                .build();
        customer1 = customerRepository.save(customer1);
        
        Customer customer2 = Customer.builder()
                .username("customer2")
                .password(passwordEncoder.encode("customer123"))
                .email("customer2@example.com")
                .role(UserRole.CUSTOMER)
                .build();
        customer2 = customerRepository.save(customer2);
        
        Asset customer1Try = Asset.builder()
                .customer(customer1)
                .assetName("TRY")
                .size(new BigDecimal("100000.00"))
                .usableSize(new BigDecimal("100000.00"))
                .build();
        assetRepository.save(customer1Try);
        
        Asset customer2Try = Asset.builder()
                .customer(customer2)
                .assetName("TRY")
                .size(new BigDecimal("50000.00"))
                .usableSize(new BigDecimal("50000.00"))
                .build();
        assetRepository.save(customer2Try);
        
        Asset customer1Aapl = Asset.builder()
                .customer(customer1)
                .assetName("AAPL")
                .size(new BigDecimal("100.00"))
                .usableSize(new BigDecimal("100.00"))
                .build();
        assetRepository.save(customer1Aapl);
        
        Asset customer1Googl = Asset.builder()
                .customer(customer1)
                .assetName("GOOGL")
                .size(new BigDecimal("50.00"))
                .usableSize(new BigDecimal("50.00"))
                .build();
        assetRepository.save(customer1Googl);
        
        Asset customer2Msft = Asset.builder()
                .customer(customer2)
                .assetName("MSFT")
                .size(new BigDecimal("75.00"))
                .usableSize(new BigDecimal("75.00"))
                .build();
        assetRepository.save(customer2Msft);
        log.info("Data initialization completed successfully");
    }
}

