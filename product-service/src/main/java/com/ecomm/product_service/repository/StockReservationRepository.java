package com.ecomm.product_service.repository;

import com.ecomm.product_service.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {
}
