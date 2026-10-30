package com.example.staj1.repository;

import com.example.staj1.model.Shipment;
import com.example.staj1.model.ShipmentStatus;
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

    Page<Shipment> findByStatusAndDeletedFalse(ShipmentStatus status, Pageable pageable);

    Page<Shipment> findBySender_IdAndDeletedFalseOrReceiver_IdAndDeletedFalse(
            Integer senderId,
            Integer receiverId,
            Pageable pageable
    );
    Optional<Shipment> findByBarcodeAndDeletedFalse(String barcode);

    Optional<Shipment> findByIdAndDeletedFalse(Integer id);

    @Query("select s from Shipment s left join s.sender sender left join s.receiver receiver " +
            "where s.barcode = :barcode and s.deleted = false and (sender.id = :userId or receiver.id = :userId)")
    Optional<Shipment> findTrackingShipment(@Param("barcode") String barcode, @Param("userId") Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.id = :id and s.deleted = false")
    Optional<Shipment> findByIdForUpdate(@Param("id") Integer id);
}