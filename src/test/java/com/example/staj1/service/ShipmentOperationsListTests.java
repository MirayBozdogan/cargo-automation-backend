package com.example.staj1.service;

import com.example.staj1.model.*;
import com.example.staj1.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentOperationsListTests {
    ShipmentRepository shipments;
    ShipmentService service;
    User operator;
    @BeforeEach void setup() {
        shipments = mock(ShipmentRepository.class);
        UserRepository users = mock(UserRepository.class);
        operator = new User(); operator.setId(7); operator.setRole("STAFF");
        when(users.findByEmail("operator@example.test")).thenReturn(Optional.of(operator));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operator@example.test", null));
        service = new ShipmentService(shipments, mock(PriceRepository.class), users, mock(AddressRepository.class));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void staffAndAdminReadAllNonDeletedShipmentsInStableOrder() {
        Shipment shipment = new Shipment(); shipment.setId(42); shipment.setSenderName("Müşteri");
        Pageable expected = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        when(shipments.findByDeletedFalse(expected)).thenReturn(new PageImpl<>(List.of(shipment), expected, 1));
        for (String role : new String[]{"STAFF", "ADMIN"}) {
            operator.setRole(role);
            var result = service.getOperations(null, 0, 10);
            assertEquals(1, result.getTotalElements()); assertEquals(42, result.getContent().getFirst().id());
            assertEquals("Müşteri", result.getContent().getFirst().senderName());
        }
        verify(shipments, never()).findByStatusAndDeletedFalse(any(), any());
    }
    @Test void filterUsesStatusAndKeepsPaginationMetadata() {
        Pageable expected = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "id"));
        when(shipments.findByStatusAndDeletedFalse(ShipmentStatus.ACCEPTED, expected))
                .thenReturn(new PageImpl<>(List.of(new Shipment()), expected, 3));
        var result = service.getOperations(ShipmentStatus.ACCEPTED, 1, 2);
        assertEquals(1, result.getNumber()); assertEquals(3, result.getTotalElements()); assertEquals(2, result.getTotalPages());
        verify(shipments, never()).findByDeletedFalse(any());
    }
    @Test void customerAndInactiveOperatorsAreDeniedBeforeReadingShipments() {
        operator.setRole("USER");
        assertThrows(AccessDeniedException.class, () -> service.getOperations(null, 0, 10));
        for (String role : new String[]{"STAFF", "ADMIN"}) {
            operator.setRole(role); operator.setActive(false);
            assertThrows(AccessDeniedException.class, () -> service.getOperations(null, 0, 10));
        }
        verifyNoInteractions(shipments);
    }
    @Test void invalidPageAndSizeAreRejectedBeforeQuery() {
        for (int[] args : new int[][]{{-1,10}, {0,0}, {0,-1}, {0,101}})
            assertThrows(IllegalArgumentException.class, () -> service.getOperations(null, args[0], args[1]));
        verifyNoInteractions(shipments);
    }
    @Test void customerListRetainsItsOwnOwnershipQuery() {
        operator.setRole("USER"); Pageable pageable = PageRequest.of(0,10);
        when(shipments.findBySender_IdAndDeletedFalseOrReceiver_IdAndDeletedFalse(7,7,pageable)).thenReturn(Page.empty(pageable));
        service.getAll(pageable);
        verify(shipments).findBySender_IdAndDeletedFalseOrReceiver_IdAndDeletedFalse(7,7,pageable);
        verify(shipments, never()).findByDeletedFalse(any());
    }
}
