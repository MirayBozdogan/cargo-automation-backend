package com.example.staj1.service;

import com.example.staj1.model.*;
import com.example.staj1.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentOperationDetailTests {
    ShipmentRepository shipments;
    ShipmentService service;
    User operator;
    Shipment shipment;
    @BeforeEach void setup() {
        shipments = mock(ShipmentRepository.class); UserRepository users = mock(UserRepository.class);
        operator = new User(); operator.setId(7); operator.setRole("STAFF");
        when(users.findByEmail("operator@example.test")).thenReturn(Optional.of(operator));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operator@example.test",null));
        service = new ShipmentService(shipments,mock(PriceRepository.class),users,mock(AddressRepository.class));
        shipment = new Shipment(); shipment.setId(42); shipment.setBarcode("TEST00000042");
        shipment.setSenderName("Kaydedilen ad"); shipment.setSenderAddressText("Kaydedilen çıkış adresi");
        shipment.setReceiverAddressText("Kaydedilen teslimat adresi"); shipment.setPrice(BigDecimal.TEN);
        shipment.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        User customer = new User(); customer.setId(9); shipment.setSender(customer);
        when(shipments.findByIdAndDeletedFalse(42)).thenReturn(Optional.of(shipment));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void activeStaffAndAdminReadOtherCustomersDetailsFromShipmentSnapshot() {
        for (String role : new String[]{"STAFF","ADMIN"}) {
            operator.setRole(role); var detail = service.getOperationDetail(42);
            assertEquals("TEST00000042",detail.barcode());
            assertEquals("Kaydedilen ad",detail.senderName());
            assertEquals("Kaydedilen çıkış adresi",detail.senderAddressText());
            assertEquals("Kaydedilen teslimat adresi",detail.receiverAddressText());
            assertEquals(BigDecimal.TEN,detail.price()); assertEquals(shipment.getCreatedAt(),detail.createdAt());
            assertEquals(ShipmentStatus.CREATED,detail.status()); assertNull(detail.deliveredAt());
        }
        verify(shipments,never()).save(any());
    }
    @Test void customersCannotReadOperatorDetailEvenForOwnShipment() {
        operator.setRole("USER"); shipment.setSender(operator);
        assertThrows(AccessDeniedException.class,()->service.getOperationDetail(42));
        verifyNoInteractions(shipments);
    }
    @Test void inactiveStaffAndAdminCannotReadDetail() {
        for (String role : new String[]{"STAFF","ADMIN"}) {
            operator.setRole(role); operator.setActive(false);
            assertThrows(AccessDeniedException.class,()->service.getOperationDetail(42));
        }
        verifyNoInteractions(shipments);
    }
    @Test void missingOrDeletedShipmentReturnsNotFound() {
        when(shipments.findByIdAndDeletedFalse(42)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,()->service.getOperationDetail(42));
    }
    @Test void invalidIdsAreRejectedBeforeQuery() {
        for (Integer id : new Integer[]{null,0,-1})
            assertThrows(IllegalArgumentException.class,()->service.getOperationDetail(id));
        verifyNoInteractions(shipments);
    }
    @Test void customerEndpointStillRequiresOwnership() {
        assertThrows(AccessDeniedException.class,()->service.getById(42));
        shipment.setSender(operator);
        assertEquals(42,service.getById(42).getId());
    }
}
