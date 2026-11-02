package com.example.staj1.repository;

import com.example.staj1.Dto.ShipmentRequest;
import com.example.staj1.model.*;
import com.example.staj1.service.ShipmentService;
import com.example.staj1.exception.ShipmentStateException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShipmentFullLifecycleTests {
    @Autowired ShipmentService service;
    @Autowired UserRepository users;
    @Autowired PriceRepository prices;
    @Autowired ShipmentRepository shipments;
    @Autowired EntityManager em;
    User user(String role) {
        User user = new User(); user.setRole(role); user.setName("Test"); user.setSurname(role);
        user.setEmail(UUID.randomUUID()+"@example.test"); return users.saveAndFlush(user);
    }
    void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void customerCreationToCourierDeliveryAndDeliveredFilterPreservesSnapshots() {
        // Bu transaction sonunda tüm test verileri geri alınır.
        prices.deleteAll();
        Price price = new Price(); price.setMinDesi(BigDecimal.ZERO); price.setMaxDesi(new BigDecimal("100"));
        price.setPrice(new BigDecimal("75")); prices.saveAndFlush(price);
        User customer = user("USER"), staff = user("STAFF"), courier = user("STAFF");
        login(customer);
        ShipmentRequest request = new ShipmentRequest();
        request.setWidth(BigDecimal.TEN); request.setLength(BigDecimal.TEN);
        request.setHeight(BigDecimal.TEN); request.setWeight(BigDecimal.ONE);
        request.setSenderCity("Ankara"); request.setSenderDistrict("Çankaya"); request.setSenderAddressText("Çıkış adresi");
        request.setReceiverName("Alıcı"); request.setReceiverSurname("Test");
        request.setReceiverCity("İstanbul"); request.setReceiverDistrict("Kadıköy"); request.setReceiverAddressText("Teslimat adresi");
        var created = service.create(request); em.flush(); em.clear();
        assertEquals(ShipmentStatus.CREATED, created.getStatus());
        assertEquals(0, new BigDecimal("75").compareTo(created.getPrice()));
        assertNotNull(created.getCreatedAt()); assertNull(created.getDeliveredAt());
        Integer id = created.getId(); String barcode = created.getBarcode();
        login(staff);
        assertEquals(ShipmentStatus.ACCEPTED, service.accept(id).getStatus());
        assertEquals(ShipmentStatus.READY_FOR_DISTRIBUTION, service.prepare(id).getStatus());
        assertEquals(ShipmentStatus.OUT_FOR_DELIVERY, service.assignCourier(id, courier.getId()).status());
        login(courier); var delivered = service.deliver(id); em.flush(); em.clear();
        assertEquals(ShipmentStatus.DELIVERED, delivered.status()); assertNotNull(delivered.deliveredAt());
        assertEquals("Çıkış adresi", delivered.senderAddressText());
        assertEquals("Teslimat adresi", delivered.receiverAddressText());
        assertEquals(created.getCreatedAt(), delivered.createdAt());
        assertThrows(ShipmentStateException.class, () -> service.deliver(id));
        var deliveredPage = service.getOperations(ShipmentStatus.DELIVERED, 0, 100);
        assertTrue(deliveredPage.getContent().stream().anyMatch(row -> row.id().equals(id)));
        assertTrue(deliveredPage.getContent().stream().allMatch(row -> row.status() == ShipmentStatus.DELIVERED));
        assertFalse(service.getOperations(ShipmentStatus.OUT_FOR_DELIVERY, 0, 100)
                .getContent().stream().anyMatch(row -> row.id().equals(id)));
        login(customer); var tracking = service.track(barcode.toLowerCase(Locale.ROOT));
        assertEquals(ShipmentStatus.DELIVERED, tracking.status()); assertEquals(delivered.deliveredAt(), tracking.deliveredAt());
        assertThrows(ShipmentStateException.class, () -> service.delete(id));
        assertThrows(ShipmentStateException.class, () -> service.update(id, request));
    }
}
