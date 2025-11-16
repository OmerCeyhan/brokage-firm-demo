package com.inghubs.brokage_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchOrderRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
}

