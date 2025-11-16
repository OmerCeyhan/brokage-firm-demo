package com.inghubs.brokage_service.service;

import com.inghubs.brokage_service.dto.response.AssetResponse;
import com.inghubs.brokage_service.exception.ForbiddenException;
import com.inghubs.brokage_service.exception.NotFoundException;
import com.inghubs.brokage_service.mapper.AssetMapper;
import com.inghubs.brokage_service.model.entity.Asset;
import com.inghubs.brokage_service.model.entity.Customer;
import com.inghubs.brokage_service.repository.AssetRepository;
import com.inghubs.brokage_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {
    
    private final AssetRepository assetRepository;
    private final CustomerRepository customerRepository;
    private final AssetMapper assetMapper;
    
    @Transactional(readOnly = true)
    public List<AssetResponse> listAssets(Long customerId, String assetName, Long authenticatedCustomerId, boolean isAdmin) {
        if (!isAdmin && authenticatedCustomerId != null && !customerId.equals(authenticatedCustomerId)) {
            throw new ForbiddenException("You can only view your own assets");
        }
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));
        
        List<Asset> assets;
        if (assetName != null && !assetName.isEmpty()) {
            Asset asset = assetRepository.findByCustomerAndAssetName(customer, assetName)
                    .orElse(null);
            assets = asset != null ? List.of(asset) : List.of();
        } else {
            assets = assetRepository.findByCustomer(customer);
        }
        
        return assetMapper.toResponseList(assets);
    }
}

