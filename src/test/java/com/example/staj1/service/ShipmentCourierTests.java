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

class ShipmentCourierTests {
    ShipmentService service;
    ShipmentRepository shipments;
    UserRepository users;
    User operator, courier;
    Shipment shipment;
    @BeforeEach void setup() {
        shipments=mock(ShipmentRepository.class); users=mock(UserRepository.class);
        operator=new User(); operator.setId(7); operator.setRole("STAFF");
        courier=new User(); courier.setId(8); courier.setRole("STAFF"); courier.setName("Kurye"); courier.setSurname("Deneme");
        when(users.findByEmail("operator@example.test")).thenReturn(Optional.of(operator));
        when(users.findActiveCourierById(8)).thenReturn(Optional.of(courier));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operator@example.test",null));
        service=new ShipmentService(shipments,mock(PriceRepository.class),users,mock(AddressRepository.class));
        shipment=new Shipment(); shipment.setId(42); shipment.setStatus(ShipmentStatus.READY_FOR_DISTRIBUTION);
        shipment.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z")); shipment.setPrice(BigDecimal.TEN); shipment.setBarcode("TEST00000042");
        when(shipments.findByIdForUpdate(42)).thenReturn(Optional.of(shipment));
        when(shipments.save(any())).thenAnswer(invocation->invocation.getArgument(0));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    void outForDelivery() { shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY); shipment.setCourier(courier); }
    @Test void staffAndAdminCanAssignActiveStaffAndPreserveShipmentDetails() {
        for (String role : new String[]{"STAFF","ADMIN"}) {
            operator.setRole(role); shipment.setStatus(ShipmentStatus.READY_FOR_DISTRIBUTION); shipment.setCourier(null);
            var result=service.assignCourier(42,8);
            assertEquals(ShipmentStatus.OUT_FOR_DELIVERY,result.status());
            assertEquals(8,result.courier().id()); assertEquals("Kurye",result.courier().name());
            assertEquals("TEST00000042",result.barcode()); assertEquals(BigDecimal.TEN,result.price());
            assertEquals(Instant.parse("2026-01-01T10:00:00Z"),result.createdAt()); assertNull(result.deliveredAt());
        }
    }
    @Test void onlyReadyShipmentsCanBeAssigned() {
        for (ShipmentStatus state : ShipmentStatus.values()) {
            if (state==ShipmentStatus.READY_FOR_DISTRIBUTION) continue;
            shipment.setStatus(state);
            assertThrows(ShipmentStateException.class,()->service.assignCourier(42,8));
        }
        verify(shipments,never()).save(any());
    }
    @Test void existingCourierOrDeliveryDateCannotBeOverwritten() {
        shipment.setCourier(courier);
        assertThrows(ShipmentStateException.class,()->service.assignCourier(42,8));
        shipment.setCourier(null); shipment.setDeliveredAt(Instant.now());
        assertThrows(ShipmentStateException.class,()->service.assignCourier(42,8));
        verify(shipments,never()).save(any());
    }
    @Test void missingInactiveOrWrongRoleCourierIsRejected() {
        when(users.findActiveCourierById(8)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,()->service.assignCourier(42,8));
        assertNull(shipment.getCourier()); assertEquals(ShipmentStatus.READY_FOR_DISTRIBUTION,shipment.getStatus());
        verify(shipments,never()).save(any());
    }
    @Test void invalidCourierIdsAreRejectedBeforeShipmentQuery() {
        for (Integer id : new Integer[]{null,0,-1}) assertThrows(IllegalArgumentException.class,()->service.assignCourier(42,id));
        verifyNoInteractions(shipments);
    }
    @Test void customerAndInactiveOperatorsCannotAssignOrDeliver() {
        operator.setRole("USER");
        assertThrows(AccessDeniedException.class,()->service.assignCourier(42,8));
        assertThrows(AccessDeniedException.class,()->service.deliver(42));
        for (String role : new String[]{"STAFF","ADMIN"}) {
            operator.setRole(role); operator.setActive(false);
            assertThrows(AccessDeniedException.class,()->service.assignCourier(42,8));
            assertThrows(AccessDeniedException.class,()->service.deliver(42));
        }
        verifyNoInteractions(shipments);
    }
    @Test void assignedStaffCanDeliverOnceWithServerTimestamp() {
        outForDelivery(); operator.setId(8); Instant before=Instant.now();
        var result=service.deliver(42);
        assertEquals(ShipmentStatus.DELIVERED,result.status()); assertNotNull(result.deliveredAt());
        assertFalse(result.deliveredAt().isBefore(before)); assertFalse(result.deliveredAt().isAfter(Instant.now()));
        Instant original=result.deliveredAt();
        assertThrows(ShipmentStateException.class,()->service.deliver(42));
        assertEquals(original,shipment.getDeliveredAt());
        verify(shipments,times(1)).save(any());
    }
    @Test void adminCanRecordDeliveryForAssignedStaff() {
        outForDelivery(); operator.setRole("ADMIN");
        assertEquals(ShipmentStatus.DELIVERED,service.deliver(42).status());
    }
    @Test void otherStaffCannotRecordDelivery() {
        outForDelivery();
        assertThrows(AccessDeniedException.class,()->service.deliver(42));
        assertNull(shipment.getDeliveredAt()); verify(shipments,never()).save(any());
    }
    @Test void deliveryRequiresOutForDeliveryAndAssignedCourier() {
        operator.setRole("ADMIN"); shipment.setCourier(courier);
        for (ShipmentStatus state : ShipmentStatus.values()) {
            if (state==ShipmentStatus.OUT_FOR_DELIVERY) continue;
            shipment.setStatus(state);
            assertThrows(ShipmentStateException.class,()->service.deliver(42));
        }
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY); shipment.setCourier(null);
        assertThrows(ShipmentStateException.class,()->service.deliver(42));
        verify(shipments,never()).save(any());
    }
    @Test void inactiveAssignedCourierBlocksDeliveryWithoutChangingDate() {
        outForDelivery(); operator.setRole("ADMIN");
        when(users.findActiveCourierById(8)).thenReturn(Optional.empty());
        assertThrows(ShipmentStateException.class,()->service.deliver(42));
        assertEquals(ShipmentStatus.OUT_FOR_DELIVERY,shipment.getStatus()); assertNull(shipment.getDeliveredAt());
    }
    @Test void missingOrDeletedShipmentIsNotFound() {
        when(shipments.findByIdForUpdate(42)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,()->service.assignCourier(42,8));
        assertThrows(EntityNotFoundException.class,()->service.deliver(42));
    }
    @Test void deliveredShipmentRejectsEveryExistingMutation() {
        outForDelivery(); shipment.setStatus(ShipmentStatus.DELIVERED); shipment.setDeliveredAt(Instant.now()); shipment.setSender(operator);
        assertThrows(ShipmentStateException.class,()->service.assignCourier(42,8));
        assertThrows(ShipmentStateException.class,()->service.accept(42));
        assertThrows(ShipmentStateException.class,()->service.prepare(42));
        assertThrows(ShipmentStateException.class,()->service.update(42,new ShipmentRequest()));
        assertThrows(ShipmentStateException.class,()->service.delete(42));
        verify(shipments,never()).save(any()); assertFalse(shipment.getDeleted());
    }
}
