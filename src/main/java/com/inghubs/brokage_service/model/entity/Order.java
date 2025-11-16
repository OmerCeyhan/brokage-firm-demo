package com.inghubs.brokage_service.model.entity;

import com.inghubs.brokage_service.model.enums.OrderSide;
import com.inghubs.brokage_service.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(name = "asset_name", nullable = false)
    private String assetName;
    
    @Column(name = "order_side", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderSide orderSide;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal size;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;
    
    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
    }
}

