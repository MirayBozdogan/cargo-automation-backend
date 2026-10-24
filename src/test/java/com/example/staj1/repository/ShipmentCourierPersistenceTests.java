package com.example.staj1.repository;

import com.example.staj1.model.*;
import com.example.staj1.exception.ShipmentStateException;
import com.example.staj1.service.ShipmentService;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShipmentCourierPersistenceTests {
    @Autowired ShipmentRepository shipments;
    @Autowired UserRepository users;
    @Autowired ShipmentService service;
    @PersistenceContext EntityManager em;
    User staff,customer;
    Integer shipmentId;
    User user(String role, boolean active) {
        User user=new User(); user.setEmail(UUID.randomUUID()+"@example.test"); user.setRole(role); user.setActive(active);
        user.setName("Deneme"); user.setSurname(role); return users.saveAndFlush(user);
    }
    void login(User user) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(),null)); }
    @BeforeEach void setup() {
        staff=user("STAFF",true); customer=user("USER",true); login(staff);
        Shipment shipment=new Shipment(); shipment.setSender(customer); shipment.setSenderName("Snapshot");
        shipment.setBarcode(UUID.randomUUID().toString().replace("-", "").substring(0,12));
        shipment.setStatus(ShipmentStatus.READY_FOR_DISTRIBUTION); shipmentId=shipments.saveAndFlush(shipment).getId();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void relationAndDeliveryPersistAndCustomerSeesUpdatedLifecycle() {
        var original=shipments.findById(shipmentId).orElseThrow().getCreatedAt();
        service.assignCourier(shipmentId,staff.getId()); em.flush(); em.clear();
        Shipment assigned=shipments.findById(shipmentId).orElseThrow();
        assertEquals(staff.getId(),assigned.getCourier().getId());
        assertEquals(ShipmentStatus.OUT_FOR_DELIVERY,assigned.getStatus()); assertNull(assigned.getDeliveredAt());
        service.deliver(shipmentId); em.flush(); em.clear();
        Shipment delivered=shipments.findById(shipmentId).orElseThrow();
        assertEquals(ShipmentStatus.DELIVERED,delivered.getStatus()); assertNotNull(delivered.getDeliveredAt());
        assertEquals(original,delivered.getCreatedAt());
        login(customer); var own=service.getById(shipmentId);
        assertEquals(ShipmentStatus.DELIVERED,own.getStatus()); assertEquals(delivered.getDeliveredAt(),own.getDeliveredAt());
        assertEquals(staff.getId(),own.getCourier().id());
    }
    @Test void courierQueryOnlyReturnsActiveStaff() {
        assertTrue(users.findActiveCourierById(staff.getId()).isPresent());
        assertTrue(users.findActiveCourierById(customer.getId()).isEmpty());
        assertTrue(users.findActiveCourierById(user("ADMIN",true).getId()).isEmpty());
        assertTrue(users.findActiveCourierById(user("STAFF",false).getId()).isEmpty());
        assertTrue(users.findActiveCourierById(Integer.MAX_VALUE).isEmpty());
    }
    @Test void foreignKeyRejectsUnknownCourier() {
        assertThrows(PersistenceException.class,()->em.createNativeQuery("UPDATE shipments SET courier_id = :courier WHERE id = :id")
                .setParameter("courier",Integer.MAX_VALUE).setParameter("id",shipmentId).executeUpdate());
    }
    @Test void inactiveAssignedCourierCannotBeDeliveredEvenByAdmin() {
        service.assignCourier(shipmentId,staff.getId());
        staff.setActive(false); users.saveAndFlush(staff); login(user("ADMIN",true));
        assertThrows(ShipmentStateException.class,()->service.deliver(shipmentId));
    }
    @Test void preExistingShipmentCanStayUnassigned() {
        em.clear(); assertNull(shipments.findById(shipmentId).orElseThrow().getCourier());
    }
}
