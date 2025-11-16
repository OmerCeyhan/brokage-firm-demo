package com.inghubs.brokage_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "assets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"customer_id", "asset_name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(name = "asset_name", nullable = false)
    private String assetName;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal size;
    
    @Column(name = "usable_size", nullable = false, precision = 19, scale = 2)
    private BigDecimal usableSize;
}

