package com.example.staj1.repository;

import com.example.staj1.Dto.ShipmentRequest;
import com.example.staj1.exception.ShipmentStateException;
import com.example.staj1.model.*;
import com.example.staj1.service.ShipmentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShipmentOperationPersistenceTests {
    @Autowired ShipmentRepository shipments;
    @Autowired UserRepository users;
    @Autowired ShipmentService service;
    @PersistenceContext EntityManager entityManager;
    User customer;
    Shipment shipment;
    void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), null));
    }
    @BeforeEach void setup() {
        customer = new User(); customer.setEmail(UUID.randomUUID()+"@example.test");
        customer = users.saveAndFlush(customer);
        User staff = new User(); staff.setEmail(UUID.randomUUID()+"@example.test"); staff.setRole("STAFF");
        login(users.saveAndFlush(staff));
        shipment = new Shipment(); shipment.setSender(customer); shipment.setPrice(BigDecimal.TEN);
        shipment.setBarcode(UUID.randomUUID().toString().replace("-", "").substring(0,12));
        shipment = shipments.saveAndFlush(shipment);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void actionsPersistStatusWhileKeepingOriginalDetails() {
        Integer id = shipment.getId(); var createdAt = shipment.getCreatedAt(); String barcode = shipment.getBarcode();
        service.accept(id); entityManager.flush(); entityManager.clear();
        assertEquals(ShipmentStatus.ACCEPTED, shipments.findById(id).orElseThrow().getStatus());
        service.prepare(id); entityManager.flush(); entityManager.clear();
        Shipment stored = shipments.findById(id).orElseThrow();
        assertEquals(ShipmentStatus.READY_FOR_DISTRIBUTION, stored.getStatus());
        assertEquals(customer.getId(), stored.getSender().getId());
        assertEquals(barcode, stored.getBarcode()); assertEquals(0, BigDecimal.TEN.compareTo(stored.getPrice()));
        assertEquals(createdAt, stored.getCreatedAt()); assertNull(stored.getDeliveredAt());
    }
    @Test void lockedLookupExcludesSoftDeletedShipments() {
        shipment.setDeleted(true); shipments.saveAndFlush(shipment); entityManager.clear();
        assertTrue(shipments.findByIdForUpdate(shipment.getId()).isEmpty());
    }
    @Test void acceptedShipmentCannotBeChangedOrDeletedByOwner() {
        Integer id = shipment.getId(); service.accept(id); entityManager.flush(); entityManager.clear();
        login(customer);
        assertThrows(ShipmentStateException.class, () -> service.update(id, new ShipmentRequest()));
        // A failed transactional call marks the transaction rollback-only; inspect the entity without committing.
        assertThrows(ShipmentStateException.class, () -> service.delete(id));
        Shipment stored = shipments.findById(id).orElseThrow();
        assertEquals(ShipmentStatus.ACCEPTED, stored.getStatus()); assertFalse(stored.getDeleted());
    }
}
