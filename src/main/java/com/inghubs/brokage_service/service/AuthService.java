package com.inghubs.brokage_service.service;

import com.inghubs.brokage_service.config.JwtUtil;
import com.inghubs.brokage_service.dto.request.LoginRequest;
import com.inghubs.brokage_service.dto.response.LoginResponse;
import com.inghubs.brokage_service.exception.UnauthorizedException;
import com.inghubs.brokage_service.model.entity.Customer;
import com.inghubs.brokage_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        
        String token = jwtUtil.generateToken(
                customer.getUsername(),
                customer.getRole().name(),
                customer.getId()
        );
        
        log.info("Login successful for user: {} with role: {}", customer.getUsername(), customer.getRole());
        return LoginResponse.builder()
                .token(token)
                .username(customer.getUsername())
                .role(customer.getRole().name())
                .build();
    }
}

