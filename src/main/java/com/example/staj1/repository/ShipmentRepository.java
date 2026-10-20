package com.example.staj1.repository;

import com.example.staj1.model.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {

    boolean existsByBarcode(String barcode);

    Page<Shipment> findByDeletedFalse(Pageable pageable);

    Page<Shipment> findBySender_IdAndDeletedFalseOrReceiver_IdAndDeletedFalse(
            Integer senderId,
            Integer receiverId,
            Pageable pageable
    );
    Optional<Shipment> findByBarcodeAndDeletedFalse(String barcode);

    Optional<Shipment> findByIdAndDeletedFalse(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.id = :id and s.deleted = false")
    Optional<Shipment> findByIdForUpdate(@Param("id") Integer id);
}