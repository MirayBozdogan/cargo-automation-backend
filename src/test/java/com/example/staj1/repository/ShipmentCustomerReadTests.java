package com.example.staj1.repository;

import com.example.staj1.model.*;
import com.example.staj1.service.ShipmentService;
import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShipmentCustomerReadTests {
    @Autowired ShipmentRepository shipments;
    @Autowired UserRepository users;
    @Autowired ShipmentService service;
    User current, other; Shipment outgoing, incoming, foreign, deleted;
    User user() {
        User user = new User(); user.setEmail(UUID.randomUUID()+"@example.test"); return users.saveAndFlush(user);
    }
    Shipment save(User sender, User receiver, boolean deleted) {
        Shipment shipment = new Shipment(); shipment.setSender(sender); shipment.setReceiver(receiver);
        shipment.setBarcode(UUID.randomUUID().toString().replace("-", "").substring(0,12));
        shipment.setDeleted(deleted); shipment.setSenderName("Kayıtlı isim"); return shipments.saveAndFlush(shipment);
    }
    @BeforeEach void setup() {
        current=user(); other=user();
        outgoing=save(current,null,false); incoming=save(null,current,false);
        foreign=save(other,null,false); deleted=save(current,null,true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(current.getEmail(),null));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void ownPageIncludesIncomingAndOutgoingAndPreservesPagination() {
        var page0=service.getAll(PageRequest.of(0,1,Sort.by(Sort.Direction.DESC,"id")));
        var page1=service.getAll(PageRequest.of(1,1,Sort.by(Sort.Direction.DESC,"id")));
        assertEquals(2,page0.getTotalElements()); assertEquals(2,page0.getTotalPages());
        assertEquals(incoming.getId(),page0.getContent().getFirst().getId());
        assertNull(page0.getContent().getFirst().getSender());
        assertEquals(current.getId(),page0.getContent().getFirst().getReceiver().getId());
        assertEquals(outgoing.getId(),page1.getContent().getFirst().getId());
        assertEquals(current.getId(),page1.getContent().getFirst().getSender().getId());
        assertNull(page1.getContent().getFirst().getReceiver());
    }
    @Test void senderAndReceiverCanReadOwnShipmentByIdOrBarcode() {
        for (Shipment shipment : new Shipment[]{outgoing,incoming}) {
            assertEquals(shipment.getId(),service.getById(shipment.getId()).getId());
            assertEquals(shipment.getId(),service.getByBarcode(shipment.getBarcode()).getId());
        }
    }
    @Test void dtoCleanupDoesNotBroadenOwnershipOrIncludeDeletedShipments() {
        assertThrows(AccessDeniedException.class,()->service.getById(foreign.getId()));
        assertThrows(AccessDeniedException.class,()->service.getByBarcode(foreign.getBarcode()));
        assertThrows(EntityNotFoundException.class,()->service.getById(deleted.getId()));
        assertThrows(EntityNotFoundException.class,()->service.getByBarcode(deleted.getBarcode()));
    }
}
