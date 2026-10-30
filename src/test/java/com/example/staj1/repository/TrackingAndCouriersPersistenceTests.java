package com.example.staj1.repository;

import com.example.staj1.model.*;
import com.example.staj1.service.ShipmentService;
import jakarta.persistence.EntityNotFoundException;
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
class TrackingAndCouriersPersistenceTests {
    @Autowired UserRepository users;
    @Autowired ShipmentRepository shipments;
    @Autowired ShipmentService service;
    User sender,receiver,outsider,staff;
    User user(String role, boolean active, String name) {
        User user=new User(); user.setRole(role); user.setActive(active); user.setName(name); user.setSurname("Deneme");
        user.setEmail(UUID.randomUUID()+"@example.test"); return users.saveAndFlush(user);
    }
    void login(User user) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(),null)); }
    Shipment shipment(User sender, User receiver, boolean deleted) {
        Shipment shipment=new Shipment(); shipment.setSender(sender); shipment.setReceiver(receiver); shipment.setDeleted(deleted);
        shipment.setBarcode(UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase());
        return shipments.saveAndFlush(shipment);
    }
    @BeforeEach void setup() {
        sender=user("USER",true,"Gönderici"); receiver=user("USER",true,"Alıcı"); outsider=user("USER",true,"Başka");
        staff=user("STAFF",true,"A");
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void senderCanTrackShipmentWithManualRecipient() {
        var row=shipment(sender,null,false); login(sender);
        assertEquals(row.getBarcode(),service.track(row.getBarcode().toLowerCase()).barcode());
    }
    @Test void receiverCanTrackEvenIfSenderIsMissing() {
        var row=shipment(null,receiver,false); login(receiver);
        assertEquals(row.getBarcode(),service.track(row.getBarcode()).barcode());
    }
    @Test void outsidersAndDeletedRowsAreHidden() {
        var row=shipment(sender,receiver,false); login(outsider);
        assertThrows(EntityNotFoundException.class,()->service.track(row.getBarcode()));
        login(sender); row.setDeleted(true); shipments.saveAndFlush(row);
        assertThrows(EntityNotFoundException.class,()->service.track(row.getBarcode()));
    }
    @Test void courierListExcludesInactiveStaffAndAllOtherRoles() {
        user("STAFF",false,"Pasif"); user("ADMIN",true,"Yönetici");
        User second=user("STAFF",true,"B"); login(staff);
        var first=service.listCouriers(0,1); var last=service.listCouriers(1,1);
        assertEquals(2,first.getTotalElements()); assertEquals(staff.getId(),first.getContent().getFirst().id());
        assertEquals(second.getId(),last.getContent().getFirst().id());
    }
    @Test void trackingReflectsCourierDeliveryAndItsTimestamp() {
        var row=shipment(sender,null,false); login(staff);
        service.accept(row.getId()); service.prepare(row.getId()); service.assignCourier(row.getId(),staff.getId());
        login(sender); assertEquals(ShipmentStatus.OUT_FOR_DELIVERY,service.track(row.getBarcode()).status());
        login(staff); var delivered=service.deliver(row.getId());
        login(sender); var tracked=service.track(row.getBarcode());
        assertEquals(ShipmentStatus.DELIVERED,tracked.status()); assertEquals(delivered.deliveredAt(),tracked.deliveredAt());
    }
}
