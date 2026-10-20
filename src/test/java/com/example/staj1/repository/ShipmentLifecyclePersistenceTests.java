package com.example.staj1.repository;

import com.example.staj1.model.Shipment;
import com.example.staj1.model.ShipmentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShipmentLifecyclePersistenceTests {
    @Autowired ShipmentRepository shipments;
    @PersistenceContext EntityManager entityManager;

    Shipment saveNew() {
        Shipment shipment = new Shipment();
        shipment.setBarcode(UUID.randomUUID().toString().replace("-", "").substring(0,12));
        return shipments.saveAndFlush(shipment);
    }
    @Test void persistenceSetsServerTimeAndStoresReadableStatus() {
        Instant before = Instant.now();
        Shipment shipment = saveNew();
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
        assertNotNull(shipment.getCreatedAt());
        assertFalse(shipment.getCreatedAt().isBefore(before));
        assertNull(shipment.getDeliveredAt());
        Object stored = entityManager.createNativeQuery("SELECT status FROM shipments WHERE id = :id", String.class)
                .setParameter("id", shipment.getId()).getSingleResult();
        assertEquals("CREATED", stored);
    }
    @Test void normalUpdatesCannotOverwriteCreationTime() {
        Shipment shipment = saveNew(); Integer id = shipment.getId();
        entityManager.clear();
        Shipment stored = shipments.findById(id).orElseThrow();
        Instant original = stored.getCreatedAt();
        stored.setCreatedAt(original.minusSeconds(3600));
        stored.setReceiverName("Updated"); shipments.saveAndFlush(stored);
        entityManager.clear();
        assertEquals(original, shipments.findById(id).orElseThrow().getCreatedAt());
    }
}
