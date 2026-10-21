package com.shopflow.inventory.repository;

import com.shopflow.inventory.entity.StockReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    boolean existsByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<StockReservation> findAllByOrderId(UUID orderId);
}
