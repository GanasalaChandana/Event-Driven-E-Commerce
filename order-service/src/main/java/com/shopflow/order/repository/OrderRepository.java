package com.shopflow.order.repository;

import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findAllByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    // Row lock for status transitions so concurrent confirm/cancel can't overwrite each other
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockById(UUID id);
}
