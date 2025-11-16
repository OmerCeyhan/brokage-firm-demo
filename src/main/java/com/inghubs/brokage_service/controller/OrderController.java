package com.inghubs.brokage_service.controller;

import com.inghubs.brokage_service.dto.request.CreateOrderRequest;
import com.inghubs.brokage_service.dto.request.MatchOrderRequest;
import com.inghubs.brokage_service.dto.response.ApiResponse;
import com.inghubs.brokage_service.dto.response.OrderResponse;
import com.inghubs.brokage_service.model.enums.OrderStatus;
import com.inghubs.brokage_service.service.OrderService;
import com.inghubs.brokage_service.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class OrderController {
    
    private final OrderService orderService;
    private final SecurityUtil securityUtil;
    
    @PostMapping
    @Operation(summary = "Create Order", description = "Create a new order for a customer")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        Long authenticatedCustomerId = securityUtil.getAuthenticatedCustomerId(httpRequest);
        boolean isAdmin = securityUtil.isAdmin();
        
        OrderResponse response = orderService.createOrder(request, authenticatedCustomerId, isAdmin);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order created successfully")
                .data(response)
                .build());
    }
    
    @GetMapping
    @Operation(summary = "List Orders", description = "List orders for a customer with optional filters")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listOrders(
            @RequestParam Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) OrderStatus status,
            HttpServletRequest httpRequest) {
        Long authenticatedCustomerId = securityUtil.getAuthenticatedCustomerId(httpRequest);
        boolean isAdmin = securityUtil.isAdmin();
        
        List<OrderResponse> responses = orderService.listOrders(
                customerId, startDate, endDate, status, authenticatedCustomerId, isAdmin);
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .success(true)
                .message("Orders retrieved successfully")
                .data(responses)
                .build());
    }
    
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Delete Order", description = "Cancel a pending order")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {
        Long authenticatedCustomerId = securityUtil.getAuthenticatedCustomerId(httpRequest);
        boolean isAdmin = securityUtil.isAdmin();
        
        orderService.deleteOrder(orderId, authenticatedCustomerId, isAdmin);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Order canceled successfully")
                .build());
    }
    
    @PostMapping("/match")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Match Order", description = "Match a pending order (Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> matchOrder(
            @Valid @RequestBody MatchOrderRequest request) {
        OrderResponse response = orderService.matchOrder(request.getOrderId());
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .success(true)
                .message("Order matched successfully")
                .data(response)
                .build());
    }
}

