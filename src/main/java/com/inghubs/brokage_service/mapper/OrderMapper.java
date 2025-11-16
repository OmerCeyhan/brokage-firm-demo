package com.inghubs.brokage_service.mapper;

import com.inghubs.brokage_service.dto.response.OrderResponse;
import com.inghubs.brokage_service.model.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    
    @Mapping(source = "customer.id", target = "customerId")
    OrderResponse toResponse(Order order);
    
    List<OrderResponse> toResponseList(List<Order> orders);
}

