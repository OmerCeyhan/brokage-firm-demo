package com.inghubs.brokage_service.service;

import com.inghubs.brokage_service.config.JwtUtil;
import com.inghubs.brokage_service.dto.request.LoginRequest;
import com.inghubs.brokage_service.dto.response.LoginResponse;
import com.inghubs.brokage_service.exception.UnauthorizedException;
import com.inghubs.brokage_service.model.entity.Customer;
import com.inghubs.brokage_service.model.enums.UserRole;
import com.inghubs.brokage_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private Customer customer;
    private LoginRequest loginRequest;
    private String hashedPassword;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        hashedPassword = "$2a$10$hashedPasswordExample";
        jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        customer = Customer.builder()
                .id(1L)
                .username("customer1")
                .password(hashedPassword)
                .email("customer1@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setUsername("customer1");
        loginRequest.setPassword("customer123");
    }

    @Test
    void login_Success() {
        when(customerRepository.findByUsername("customer1")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("customer123", hashedPassword)).thenReturn(true);
        when(jwtUtil.generateToken("customer1", "CUSTOMER", 1L)).thenReturn(jwtToken);

        LoginResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals(jwtToken, result.getToken());
        assertEquals("customer1", result.getUsername());
        assertEquals("CUSTOMER", result.getRole());
        verify(customerRepository).findByUsername("customer1");
        verify(passwordEncoder).matches("customer123", hashedPassword);
        verify(jwtUtil).generateToken("customer1", "CUSTOMER", 1L);
    }

    @Test
    void login_AdminUser_Success() {
        Customer admin = Customer.builder()
                .id(2L)
                .username("admin")
                .password(hashedPassword)
                .email("admin@brokage.com")
                .role(UserRole.ADMIN)
                .build();

        LoginRequest adminRequest = new LoginRequest();
        adminRequest.setUsername("admin");
        adminRequest.setPassword("admin123");

        when(customerRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", hashedPassword)).thenReturn(true);
        when(jwtUtil.generateToken("admin", "ADMIN", 2L)).thenReturn(jwtToken);

        LoginResponse result = authService.login(adminRequest);

        assertNotNull(result);
        assertEquals("ADMIN", result.getRole());
        verify(jwtUtil).generateToken("admin", "ADMIN", 2L);
    }

    @Test
    void login_UserNotFound_ThrowsUnauthorizedException() {
        when(customerRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("nonexistent");
        invalidRequest.setPassword("password");

        assertThrows(UnauthorizedException.class, () -> authService.login(invalidRequest));
        verify(customerRepository).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_InvalidPassword_ThrowsUnauthorizedException() {
        when(customerRepository.findByUsername("customer1")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrongpassword", hashedPassword)).thenReturn(false);

        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("customer1");
        invalidRequest.setPassword("wrongpassword");

        assertThrows(UnauthorizedException.class, () -> authService.login(invalidRequest));
        verify(customerRepository).findByUsername("customer1");
        verify(passwordEncoder).matches("wrongpassword", hashedPassword);
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_EmptyUsername_ThrowsUnauthorizedException() {
        when(customerRepository.findByUsername("")).thenReturn(Optional.empty());

        LoginRequest emptyRequest = new LoginRequest();
        emptyRequest.setUsername("");
        emptyRequest.setPassword("password");

        assertThrows(UnauthorizedException.class, () -> authService.login(emptyRequest));
        verify(customerRepository).findByUsername("");
    }

    @Test
    void login_NullUsername_ThrowsUnauthorizedException() {
        when(customerRepository.findByUsername(null)).thenReturn(Optional.empty());

        LoginRequest nullRequest = new LoginRequest();
        nullRequest.setUsername(null);
        nullRequest.setPassword("password");

        assertThrows(UnauthorizedException.class, () -> authService.login(nullRequest));
        verify(customerRepository).findByUsername(null);
    }
}

