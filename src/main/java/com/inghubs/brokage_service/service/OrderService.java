package com.inghubs.brokage_service.service;

import com.inghubs.brokage_service.dto.request.CreateOrderRequest;
import com.inghubs.brokage_service.dto.response.OrderResponse;
import com.inghubs.brokage_service.exception.BadRequestException;
import com.inghubs.brokage_service.exception.ForbiddenException;
import com.inghubs.brokage_service.exception.NotFoundException;
import com.inghubs.brokage_service.mapper.OrderMapper;
import com.inghubs.brokage_service.model.entity.Asset;
import com.inghubs.brokage_service.model.entity.Customer;
import com.inghubs.brokage_service.model.entity.Order;
import com.inghubs.brokage_service.model.enums.OrderSide;
import com.inghubs.brokage_service.model.enums.OrderStatus;
import com.inghubs.brokage_service.repository.AssetRepository;
import com.inghubs.brokage_service.repository.CustomerRepository;
import com.inghubs.brokage_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private static final String TRY_ASSET = "TRY";
    
    private final OrderRepository orderRepository;
    private final AssetRepository assetRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;
    
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long authenticatedCustomerId, boolean isAdmin) {
        if (!isAdmin && authenticatedCustomerId != null && !request.getCustomerId().equals(authenticatedCustomerId)) {
            throw new ForbiddenException("You can only create orders for yourself");
        }
        
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + request.getCustomerId()));
        
        if (request.getOrderSide() == OrderSide.BUY) {
            validateAndReserveAsset(customer, TRY_ASSET, request.getSize().multiply(request.getPrice()));
        } else {
            validateAndReserveAsset(customer, request.getAssetName(), request.getSize());
        }
        
        Order order = Order.builder()
                .customer(customer)
                .assetName(request.getAssetName())
                .orderSide(request.getOrderSide())
                .size(request.getSize())
                .price(request.getPrice())
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {} for customer: {}", savedOrder.getId(), savedOrder.getCustomer().getId());
        return orderMapper.toResponse(savedOrder);
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long customerId, LocalDateTime startDate, LocalDateTime endDate, 
                                         OrderStatus status, Long authenticatedCustomerId, boolean isAdmin) {
        if (!isAdmin && authenticatedCustomerId != null && !customerId.equals(authenticatedCustomerId)) {
            throw new ForbiddenException("You can only view your own orders");
        }
        
        List<Order> orders = orderRepository.findByCustomerIdAndFilters(customerId, startDate, endDate, status);
        return orderMapper.toResponseList(orders);
    }
    
    @Transactional
    public void deleteOrder(Long orderId, Long authenticatedCustomerId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));
        
        if (!isAdmin && authenticatedCustomerId != null && !order.getCustomer().getId().equals(authenticatedCustomerId)) {
            throw new ForbiddenException("You can only delete your own orders");
        }
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only PENDING orders can be deleted");
        }
        
        releaseReservedAssets(order);
        
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        log.info("Order ID: {} canceled successfully", orderId);
    }
    
    @Transactional
    public OrderResponse matchOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only PENDING orders can be matched");
        }
        
        executeOrder(order);
        
        order.setStatus(OrderStatus.MATCHED);
        Order savedOrder = orderRepository.save(order);
        log.info("Order ID: {} matched successfully", orderId);
        return orderMapper.toResponse(savedOrder);
    }
    
    private void validateAndReserveAsset(Customer customer, String assetName, BigDecimal requiredAmount) {
        Asset asset = assetRepository.findByCustomerIdAndAssetNameWithLock(customer.getId(), assetName)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + assetName + " for customer: " + customer.getId()));
        
        if (asset.getUsableSize().compareTo(requiredAmount) < 0) {
            throw new BadRequestException("Insufficient usable size for asset: " + assetName + 
                    ". Required: " + requiredAmount + ", Available: " + asset.getUsableSize());
        }
        
        asset.setUsableSize(asset.getUsableSize().subtract(requiredAmount));
        assetRepository.save(asset);
    }
    
    private void releaseReservedAssets(Order order) {
        if (order.getOrderSide() == OrderSide.BUY) {
            releaseAsset(order.getCustomer().getId(), TRY_ASSET, order.getSize().multiply(order.getPrice()));
        } else {
            releaseAsset(order.getCustomer().getId(), order.getAssetName(), order.getSize());
        }
    }
    
    private void releaseAsset(Long customerId, String assetName, BigDecimal amount) {
        Asset asset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, assetName)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + assetName + " for customer: " + customerId));
        
        asset.setUsableSize(asset.getUsableSize().add(amount));
        assetRepository.save(asset);
    }
    
    private void executeOrder(Order order) {
        if (order.getOrderSide() == OrderSide.BUY) {
            executeBuyOrder(order);
        } else {
            executeSellOrder(order);
        }
    }
    
    private void executeBuyOrder(Order order) {
        BigDecimal totalCost = order.getSize().multiply(order.getPrice());
        Long customerId = order.getCustomer().getId();
        
        Asset tryAsset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, TRY_ASSET)
                .orElseThrow(() -> new NotFoundException("TRY asset not found for customer: " + customerId));
        tryAsset.setSize(tryAsset.getSize().subtract(totalCost));
        assetRepository.save(tryAsset);
        
        Asset boughtAsset = assetRepository.findByCustomerIdAndAssetNameWithLock(
                customerId, order.getAssetName()).orElse(null);
        
        if (boughtAsset == null) {
            boughtAsset = Asset.builder()
                    .customer(order.getCustomer())
                    .assetName(order.getAssetName())
                    .size(order.getSize())
                    .usableSize(order.getSize())
                    .build();
        } else {
            boughtAsset.setSize(boughtAsset.getSize().add(order.getSize()));
            boughtAsset.setUsableSize(boughtAsset.getUsableSize().add(order.getSize()));
        }
        assetRepository.save(boughtAsset);
    }
    
    private void executeSellOrder(Order order) {
        BigDecimal totalRevenue = order.getSize().multiply(order.getPrice());
        Long customerId = order.getCustomer().getId();
        
        Asset soldAsset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, order.getAssetName())
                .orElseThrow(() -> new NotFoundException("Asset not found: " + order.getAssetName() + " for customer: " + customerId));
        soldAsset.setSize(soldAsset.getSize().subtract(order.getSize()));
        assetRepository.save(soldAsset);
        
        Asset tryAsset = assetRepository.findByCustomerIdAndAssetNameWithLock(customerId, TRY_ASSET)
                .orElseThrow(() -> new NotFoundException("TRY asset not found for customer: " + customerId));
        tryAsset.setSize(tryAsset.getSize().add(totalRevenue));
        tryAsset.setUsableSize(tryAsset.getUsableSize().add(totalRevenue));
        assetRepository.save(tryAsset);
    }
}

