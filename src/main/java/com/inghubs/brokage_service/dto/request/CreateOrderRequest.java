package com.inghubs.brokage_service.dto.request;

import com.inghubs.brokage_service.model.enums.OrderSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotBlank(message = "Asset name is required")
    private String assetName;
    
    @NotNull(message = "Order side is required")
    private OrderSide orderSide;
    
    @NotNull(message = "Size is required")
    @Positive(message = "Size must be positive")
    private BigDecimal size;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    private BigDecimal price;
}

