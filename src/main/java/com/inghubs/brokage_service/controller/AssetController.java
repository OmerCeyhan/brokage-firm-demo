package com.inghubs.brokage_service.controller;

import com.inghubs.brokage_service.dto.response.ApiResponse;
import com.inghubs.brokage_service.dto.response.AssetResponse;
import com.inghubs.brokage_service.service.AssetService;
import com.inghubs.brokage_service.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Asset management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AssetController {
    
    private final AssetService assetService;
    private final SecurityUtil securityUtil;
    
    @GetMapping
    @Operation(summary = "List Assets", description = "List assets for a customer with optional filters")
    public ResponseEntity<ApiResponse<List<AssetResponse>>> listAssets(
            @RequestParam Long customerId,
            @RequestParam(required = false) String assetName,
            HttpServletRequest httpRequest) {
        Long authenticatedCustomerId = securityUtil.getAuthenticatedCustomerId(httpRequest);
        boolean isAdmin = securityUtil.isAdmin();
        
        List<AssetResponse> responses = assetService.listAssets(
                customerId, assetName, authenticatedCustomerId, isAdmin);
        return ResponseEntity.ok(ApiResponse.<List<AssetResponse>>builder()
                .success(true)
                .message("Assets retrieved successfully")
                .data(responses)
                .build());
    }
}

