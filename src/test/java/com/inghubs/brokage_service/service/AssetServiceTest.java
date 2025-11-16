package com.inghubs.brokage_service.service;

import com.inghubs.brokage_service.dto.response.AssetResponse;
import com.inghubs.brokage_service.exception.ForbiddenException;
import com.inghubs.brokage_service.exception.NotFoundException;
import com.inghubs.brokage_service.mapper.AssetMapper;
import com.inghubs.brokage_service.model.entity.Asset;
import com.inghubs.brokage_service.model.entity.Customer;
import com.inghubs.brokage_service.model.enums.UserRole;
import com.inghubs.brokage_service.repository.AssetRepository;
import com.inghubs.brokage_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AssetMapper assetMapper;

    @InjectMocks
    private AssetService assetService;

    private Customer customer;
    private Asset tryAsset;
    private Asset aaplAsset;
    private AssetResponse tryAssetResponse;
    private AssetResponse aaplAssetResponse;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .username("customer1")
                .email("customer1@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        tryAsset = Asset.builder()
                .id(1L)
                .customer(customer)
                .assetName("TRY")
                .size(new BigDecimal("100000.00"))
                .usableSize(new BigDecimal("100000.00"))
                .build();

        aaplAsset = Asset.builder()
                .id(2L)
                .customer(customer)
                .assetName("AAPL")
                .size(new BigDecimal("100.00"))
                .usableSize(new BigDecimal("100.00"))
                .build();

        tryAssetResponse = AssetResponse.builder()
                .id(1L)
                .customerId(1L)
                .assetName("TRY")
                .size(new BigDecimal("100000.00"))
                .usableSize(new BigDecimal("100000.00"))
                .build();

        aaplAssetResponse = AssetResponse.builder()
                .id(2L)
                .customerId(1L)
                .assetName("AAPL")
                .size(new BigDecimal("100.00"))
                .usableSize(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void listAssets_AllAssets_Success() {
        Long customerId = 1L;
        List<Asset> assets = List.of(tryAsset, aaplAsset);
        List<AssetResponse> assetResponses = List.of(tryAssetResponse, aaplAssetResponse);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(assetRepository.findByCustomer(customer)).thenReturn(assets);
        when(assetMapper.toResponseList(assets)).thenReturn(assetResponses);

        List<AssetResponse> result = assetService.listAssets(customerId, null, 1L, false);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(customerRepository).findById(customerId);
        verify(assetRepository).findByCustomer(customer);
        verify(assetMapper).toResponseList(assets);
    }

    @Test
    void listAssets_WithAssetNameFilter_Success() {
        Long customerId = 1L;
        String assetName = "AAPL";

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(assetRepository.findByCustomerAndAssetName(customer, assetName))
                .thenReturn(Optional.of(aaplAsset));
        when(assetMapper.toResponseList(List.of(aaplAsset)))
                .thenReturn(List.of(aaplAssetResponse));

        List<AssetResponse> result = assetService.listAssets(customerId, assetName, 1L, false);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(customerRepository).findById(customerId);
        verify(assetRepository).findByCustomerAndAssetName(customer, assetName);
    }

    @Test
    void listAssets_WithAssetNameFilter_AssetNotFound_ReturnsEmptyList() {
        Long customerId = 1L;
        String assetName = "GOOGL";

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(assetRepository.findByCustomerAndAssetName(customer, assetName))
                .thenReturn(Optional.empty());
        when(assetMapper.toResponseList(List.of()))
                .thenReturn(List.of());

        List<AssetResponse> result = assetService.listAssets(customerId, assetName, 1L, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(assetRepository).findByCustomerAndAssetName(customer, assetName);
    }

    @Test
    void listAssets_CustomerNotFound_ThrowsNotFoundException() {
        Long customerId = 999L;

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
                assetService.listAssets(customerId, null, 1L, true));
        verify(customerRepository).findById(customerId);
        verify(assetRepository, never()).findByCustomer(any());
    }

    @Test
    void listAssets_NonAdminTryingToViewOtherCustomerAssets_ThrowsForbiddenException() {
        Long customerId = 2L;

        assertThrows(ForbiddenException.class, () -> 
                assetService.listAssets(customerId, null, 1L, false));
        verify(customerRepository, never()).findById(any());
        verify(assetRepository, never()).findByCustomer(any());
    }

    @Test
    void listAssets_AdminCanViewAnyCustomerAssets_Success() {
        Long customerId = 2L;
        Customer otherCustomer = Customer.builder()
                .id(2L)
                .username("customer2")
                .build();
        List<Asset> assets = List.of(tryAsset);
        List<AssetResponse> assetResponses = List.of(tryAssetResponse);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(otherCustomer));
        when(assetRepository.findByCustomer(otherCustomer)).thenReturn(assets);
        when(assetMapper.toResponseList(assets)).thenReturn(assetResponses);

        List<AssetResponse> result = assetService.listAssets(customerId, null, 1L, true);

        assertNotNull(result);
        verify(customerRepository).findById(customerId);
        verify(assetRepository).findByCustomer(otherCustomer);
    }

    @Test
    void listAssets_EmptyAssetNameFilter_ReturnsAllAssets() {
        Long customerId = 1L;
        String assetName = "";
        List<Asset> assets = List.of(tryAsset, aaplAsset);
        List<AssetResponse> assetResponses = List.of(tryAssetResponse, aaplAssetResponse);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(assetRepository.findByCustomer(customer)).thenReturn(assets);
        when(assetMapper.toResponseList(assets)).thenReturn(assetResponses);

        List<AssetResponse> result = assetService.listAssets(customerId, assetName, 1L, false);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(assetRepository).findByCustomer(customer);
        verify(assetRepository, never()).findByCustomerAndAssetName(any(), any());
    }
}

