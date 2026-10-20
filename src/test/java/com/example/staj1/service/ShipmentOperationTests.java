package com.example.staj1.service;

import com.example.staj1.Dto.ShipmentRequest;
import com.example.staj1.exception.ShipmentStateException;
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

class ShipmentOperationTests {
    ShipmentRepository shipments;
    ShipmentService service;
    User operator;
    Shipment shipment;
    @BeforeEach void setup() {
        shipments = mock(ShipmentRepository.class);
        UserRepository users = mock(UserRepository.class);
        operator = new User(); operator.setId(7); operator.setRole("STAFF");
        when(users.findByEmail("operator@example.test")).thenReturn(Optional.of(operator));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("operator@example.test", null));
        service = new ShipmentService(shipments, mock(PriceRepository.class), users, mock(AddressRepository.class));
        User customer = new User(); customer.setId(9);
        shipment = new Shipment(); shipment.setId(42); shipment.setSender(customer);
        shipment.setBarcode("DAY181900001"); shipment.setPrice(BigDecimal.TEN);
        shipment.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        when(shipments.findByIdForUpdate(42)).thenReturn(Optional.of(shipment));
        when(shipments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void staffAcceptsAndPreparesCustomerShipmentWithoutChangingOtherFields() {
        var accepted = service.accept(42);
        assertEquals(ShipmentStatus.ACCEPTED, accepted.getStatus());
        var prepared = service.prepare(42);
        assertEquals(ShipmentStatus.READY_FOR_DISTRIBUTION, prepared.getStatus());
        assertEquals(9, prepared.getSender().getId());
        assertEquals("DAY181900001", prepared.getBarcode());
        assertEquals(BigDecimal.TEN, prepared.getPrice());
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), prepared.getCreatedAt());
        assertNull(prepared.getDeliveredAt());
        verify(shipments, times(2)).findByIdForUpdate(42);
    }
    @Test void adminCanPerformBothActions() {
        operator.setRole("ADMIN");
        assertEquals(ShipmentStatus.ACCEPTED, service.accept(42).getStatus());
        assertEquals(ShipmentStatus.READY_FOR_DISTRIBUTION, service.prepare(42).getStatus());
    }
    @Test void customersCannotPerformEitherActionEvenForOwnShipment() {
        operator.setRole("USER"); shipment.setSender(operator);
        assertThrows(AccessDeniedException.class, () -> service.accept(42));
        assertThrows(AccessDeniedException.class, () -> service.prepare(42));
        verify(shipments, never()).findByIdForUpdate(any());
        verify(shipments, never()).save(any());
    }
    @Test void inactiveOperatorsCannotPerformEitherAction() {
        for (String role : new String[]{"STAFF", "ADMIN"}) {
            operator.setRole(role); operator.setActive(false);
            assertThrows(AccessDeniedException.class, () -> service.accept(42));
            assertThrows(AccessDeniedException.class, () -> service.prepare(42));
        }
        verify(shipments, never()).save(any());
    }
    @Test void prepareCannotSkipAcceptance() {
        assertThrows(ShipmentStateException.class, () -> service.prepare(42));
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
        verify(shipments, never()).save(any());
    }
    @Test void repeatedActionsAreRejected() {
        service.accept(42);
        assertThrows(ShipmentStateException.class, () -> service.accept(42));
        service.prepare(42);
        assertThrows(ShipmentStateException.class, () -> service.prepare(42));
        verify(shipments, times(2)).save(any());
    }
    @Test void laterStatesCannotMoveBackwards() {
        for (ShipmentStatus state : new ShipmentStatus[]{ShipmentStatus.READY_FOR_DISTRIBUTION,
                ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED}) {
            shipment.setStatus(state);
            assertThrows(ShipmentStateException.class, () -> service.accept(42));
            assertThrows(ShipmentStateException.class, () -> service.prepare(42));
            assertEquals(state, shipment.getStatus());
        }
        verify(shipments, never()).save(any());
    }
    @Test void missingOrDeletedShipmentIsNotFound() {
        when(shipments.findByIdForUpdate(42)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.accept(42));
        assertThrows(EntityNotFoundException.class, () -> service.prepare(42));
    }
    @Test void acceptedAndLaterShipmentsCannotBeEditedOrDeletedBySender() {
        shipment.setSender(operator);
        for (ShipmentStatus state : ShipmentStatus.values()) {
            if (state == ShipmentStatus.CREATED) continue;
            shipment.setStatus(state);
            assertThrows(ShipmentStateException.class, () -> service.update(42, new ShipmentRequest()));
            assertThrows(ShipmentStateException.class, () -> service.delete(42));
            assertFalse(shipment.getDeleted());
            assertEquals(state, shipment.getStatus());
        }
        verify(shipments, never()).save(any());
    }
    @Test void statusActionsDoNotGrantOwnershipForEditingOrDeleting() {
        for (String role : new String[]{"USER", "STAFF", "ADMIN"}) {
            operator.setRole(role);
            assertThrows(AccessDeniedException.class, () -> service.update(42, new ShipmentRequest()));
            assertThrows(AccessDeniedException.class, () -> service.delete(42));
        }
        verify(shipments, never()).save(any());
    }
    @Test void senderCanStillSoftDeleteCreatedShipment() {
        shipment.setSender(operator);
        service.delete(42);
        assertTrue(shipment.getDeleted());
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
        verify(shipments).save(shipment);
    }
    @Test void inactiveSenderCannotEditOrDelete() {
        shipment.setSender(operator); operator.setActive(false);
        assertThrows(AccessDeniedException.class, () -> service.update(42, new ShipmentRequest()));
        assertThrows(AccessDeniedException.class, () -> service.delete(42));
        verify(shipments, never()).save(any());
    }
}
