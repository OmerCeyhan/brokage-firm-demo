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
import com.inghubs.brokage_service.model.enums.UserRole;
import com.inghubs.brokage_service.repository.AssetRepository;
import com.inghubs.brokage_service.repository.CustomerRepository;
import com.inghubs.brokage_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Asset tryAsset;
    private Asset aaplAsset;
    private Order order;
    private OrderResponse orderResponse;

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

        order = Order.builder()
                .id(1L)
                .customer(customer)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("10.00"))
                .price(new BigDecimal("150.00"))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        orderResponse = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("10.00"))
                .price(new BigDecimal("150.00"))
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    void createOrder_BuyOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setAssetName("AAPL");
        request.setOrderSide(OrderSide.BUY);
        request.setSize(new BigDecimal("10.00"));
        request.setPrice(new BigDecimal("150.00"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(1L), eq("TRY")))
                .thenReturn(Optional.of(tryAsset));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(request, 1L, false);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(customerRepository).findById(1L);
        verify(assetRepository).findByCustomerIdAndAssetNameWithLock(1L, "TRY");
        verify(assetRepository).save(any(Asset.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_SellOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setAssetName("AAPL");
        request.setOrderSide(OrderSide.SELL);
        request.setSize(new BigDecimal("5.00"));
        request.setPrice(new BigDecimal("150.00"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(1L), eq("AAPL")))
                .thenReturn(Optional.of(aaplAsset));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(request, 1L, false);

        assertNotNull(result);
        verify(assetRepository).findByCustomerIdAndAssetNameWithLock(1L, "AAPL");
    }

    @Test
    void createOrder_CustomerNotFound_ThrowsNotFoundException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(999L);
        request.setAssetName("AAPL");
        request.setOrderSide(OrderSide.BUY);
        request.setSize(new BigDecimal("10.00"));
        request.setPrice(new BigDecimal("150.00"));

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.createOrder(request, 1L, true));
        verify(customerRepository).findById(999L);
        verify(assetRepository, never()).findByCustomerIdAndAssetNameWithLock(any(), any());
    }

    @Test
    void createOrder_InsufficientUsableSize_ThrowsBadRequestException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setAssetName("AAPL");
        request.setOrderSide(OrderSide.BUY);
        request.setSize(new BigDecimal("1000.00"));
        request.setPrice(new BigDecimal("150.00"));

        Asset insufficientAsset = Asset.builder()
                .id(1L)
                .customer(customer)
                .assetName("TRY")
                .size(new BigDecimal("100000.00"))
                .usableSize(new BigDecimal("100.00"))
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(1L), eq("TRY")))
                .thenReturn(Optional.of(insufficientAsset));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(request, 1L, false));
        verify(assetRepository).findByCustomerIdAndAssetNameWithLock(1L, "TRY");
    }

    @Test
    void createOrder_NonAdminTryingToCreateForOtherCustomer_ThrowsForbiddenException() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(2L);
        request.setAssetName("AAPL");
        request.setOrderSide(OrderSide.BUY);
        request.setSize(new BigDecimal("10.00"));
        request.setPrice(new BigDecimal("150.00"));

        assertThrows(ForbiddenException.class, () -> orderService.createOrder(request, 1L, false));
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void createOrder_AdminCanCreateForAnyCustomer_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(2L);
        request.setAssetName("AAPL");
        request.setOrderSide(OrderSide.BUY);
        request.setSize(new BigDecimal("10.00"));
        request.setPrice(new BigDecimal("150.00"));

        Customer otherCustomer = Customer.builder()
                .id(2L)
                .username("customer2")
                .build();

        when(customerRepository.findById(2L)).thenReturn(Optional.of(otherCustomer));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(2L), eq("TRY")))
                .thenReturn(Optional.of(tryAsset));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(request, 1L, true);

        assertNotNull(result);
        verify(customerRepository).findById(2L);
    }

    @Test
    void listOrders_Success() {
        Long customerId = 1L;
        List<Order> orders = List.of(order);
        List<OrderResponse> orderResponses = List.of(orderResponse);

        when(orderRepository.findByCustomerIdAndFilters(eq(customerId), any(), any(), any()))
                .thenReturn(orders);
        when(orderMapper.toResponseList(orders)).thenReturn(orderResponses);

        List<OrderResponse> result = orderService.listOrders(customerId, null, null, null, 1L, false);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findByCustomerIdAndFilters(customerId, null, null, null);
    }

    @Test
    void listOrders_NonAdminTryingToViewOtherCustomerOrders_ThrowsForbiddenException() {
        Long customerId = 2L;

        assertThrows(ForbiddenException.class, () -> 
                orderService.listOrders(customerId, null, null, null, 1L, false));
        verify(orderRepository, never()).findByCustomerIdAndFilters(any(), any(), any(), any());
    }

    @Test
    void deleteOrder_Success() {
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(1L), eq("TRY")))
                .thenReturn(Optional.of(tryAsset));

        orderService.deleteOrder(orderId, 1L, false);

        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELED, order.getStatus());
    }

    @Test
    void deleteOrder_OrderNotFound_ThrowsNotFoundException() {
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.deleteOrder(orderId, 1L, false));
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteOrder_NonPendingOrder_ThrowsBadRequestException() {
        Long orderId = 1L;
        order.setStatus(OrderStatus.MATCHED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> orderService.deleteOrder(orderId, 1L, false));
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteOrder_NonAdminTryingToDeleteOtherCustomerOrder_ThrowsForbiddenException() {
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class, () -> orderService.deleteOrder(orderId, 2L, false));
        verify(orderRepository).findById(orderId);
    }

    @Test
    void matchOrder_BuyOrder_Success() {
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(1L), eq("TRY")))
                .thenReturn(Optional.of(tryAsset));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(1L), eq("AAPL")))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.matchOrder(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.MATCHED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void matchOrder_SellOrder_Success() {
        Long orderId = 1L;
        order.setOrderSide(OrderSide.SELL);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(1L), eq("AAPL")))
                .thenReturn(Optional.of(aaplAsset));
        when(assetRepository.findByCustomerIdAndAssetNameWithLock(eq(1L), eq("TRY")))
                .thenReturn(Optional.of(tryAsset));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.matchOrder(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.MATCHED, order.getStatus());
    }

    @Test
    void matchOrder_OrderNotFound_ThrowsNotFoundException() {
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.matchOrder(orderId));
        verify(orderRepository).findById(orderId);
    }

    @Test
    void matchOrder_NonPendingOrder_ThrowsBadRequestException() {
        Long orderId = 1L;
        order.setStatus(OrderStatus.MATCHED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> orderService.matchOrder(orderId));
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
    }
}

