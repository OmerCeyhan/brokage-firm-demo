package com.inghubs.brokage_service.repository;

import com.inghubs.brokage_service.model.entity.Order;
import com.inghubs.brokage_service.model.entity.Customer;
import com.inghubs.brokage_service.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(Customer customer);
    
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId " +
           "AND (:startDate IS NULL OR o.createDate >= :startDate) " +
           "AND (:endDate IS NULL OR o.createDate <= :endDate) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "ORDER BY o.createDate DESC")
    List<Order> findByCustomerIdAndFilters(
        @Param("customerId") Long customerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") OrderStatus status
    );
    
    List<Order> findByStatus(OrderStatus status);
}

