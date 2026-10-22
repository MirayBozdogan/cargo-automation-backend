package com.example.staj1.repository;

import com.example.staj1.model.*;
import com.example.staj1.service.ShipmentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShipmentOperationsPersistenceTests {
    @Autowired ShipmentRepository shipments;
    @Autowired UserRepository users;
    @Autowired ShipmentService service;
    Integer newestId;
    @BeforeEach void setup() {
        User staff = new User(); staff.setRole("STAFF"); staff.setEmail(UUID.randomUUID()+"@example.test"); users.saveAndFlush(staff);
        User customer = new User(); customer.setEmail(UUID.randomUUID()+"@example.test"); users.saveAndFlush(customer);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(staff.getEmail(),null));
        save(customer,ShipmentStatus.CREATED,false);
        save(customer,ShipmentStatus.ACCEPTED,false);
        newestId = save(customer,ShipmentStatus.ACCEPTED,false).getId();
        save(customer,ShipmentStatus.ACCEPTED,true);
    }
    Shipment save(User sender, ShipmentStatus status, boolean deleted) {
        Shipment shipment = new Shipment(); shipment.setSender(sender); shipment.setSenderName("Müşteri");
        shipment.setBarcode(UUID.randomUUID().toString().replace("-", "").substring(0,12));
        shipment.setStatus(status); shipment.setDeleted(deleted); return shipments.saveAndFlush(shipment);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void operatorsSeeOtherCustomersShipmentsButNotDeletedRows() {
        var result = service.getOperations(null,0,10);
        assertEquals(3,result.getTotalElements());
        assertEquals(newestId,result.getContent().getFirst().id());
        assertTrue(result.getContent().stream().allMatch(row -> "Müşteri".equals(row.senderName())));
    }
    @Test void filteredCountAndPagesExcludeDeletedRows() {
        var first = service.getOperations(ShipmentStatus.ACCEPTED,0,1);
        var second = service.getOperations(ShipmentStatus.ACCEPTED,1,1);
        assertEquals(2,first.getTotalElements()); assertEquals(2,first.getTotalPages());
        assertEquals(newestId,first.getContent().getFirst().id());
        assertNotEquals(first.getContent().getFirst().id(),second.getContent().getFirst().id());
        assertEquals(ShipmentStatus.ACCEPTED,second.getContent().getFirst().status());
    }
    @Test void stateWithNoShipmentsReturnsEmptyPage() {
        var result = service.getOperations(ShipmentStatus.DELIVERED,0,10);
        assertTrue(result.isEmpty()); assertEquals(0,result.getTotalElements());
    }
}
