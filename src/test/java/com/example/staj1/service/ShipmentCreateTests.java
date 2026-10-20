package com.example.staj1.service;

import com.example.staj1.Dto.ShipmentRequest;
import com.example.staj1.model.User;
import com.example.staj1.model.Shipment;
import com.example.staj1.model.Price;
import com.example.staj1.model.ShipmentStatus;
import java.time.Instant;
import jakarta.persistence.EntityNotFoundException;
import com.example.staj1.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentCreateTests {
    ShipmentRepository shipments;
    ShipmentService service;
    PriceRepository prices;
    User user;
    @BeforeEach void setup() {
        shipments = mock(ShipmentRepository.class);
        UserRepository users = mock(UserRepository.class);
        user = new User(); user.setId(7); user.setName("Deneme"); user.setSurname("Musteri"); user.setRole("USER");
        when(users.findByEmail("test@example.test")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test@example.test", null));
        prices = mock(PriceRepository.class);
        service = spy(new ShipmentService(shipments, prices, users, mock(AddressRepository.class)));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    ShipmentRequest request() {
        ShipmentRequest r = new ShipmentRequest();
        r.setWidth(BigDecimal.TEN); r.setLength(BigDecimal.TEN); r.setHeight(BigDecimal.TEN); r.setWeight(BigDecimal.ONE);
        r.setSenderCity("Ankara"); r.setSenderDistrict("Cankaya"); r.setSenderAddressText("Deneme adres");
        r.setReceiverName("Alici"); r.setReceiverSurname("Deneme"); r.setReceiverCity("Istanbul"); r.setReceiverDistrict("Kadikoy"); r.setReceiverAddressText("Alici adres");
        return r;
    }
    @Test void senderComesFromSessionWithoutClientId() {
        ShipmentRequest r = request();
        doReturn(BigDecimal.TEN).when(service).calculatePrice(any(), any(), any(), any());
        when(shipments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(r);
        assertEquals(user.getId(), result.getSender().getId());
        assertEquals(user.getName(), result.getSenderName());
        assertEquals(ShipmentStatus.CREATED, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNull(result.getDeliveredAt());
    }
    @Test void cannotImpersonateAnotherSender() {
        ShipmentRequest r = request(); r.setSenderId(8);
        assertThrows(AccessDeniedException.class, () -> service.create(r));
        verifyNoInteractions(shipments);
    }
    @Test void customerCannotSelectRegisteredRecipient() {
        ShipmentRequest r = request(); r.setReceiverId(8);
        assertThrows(AccessDeniedException.class, () -> service.create(r));
        verifyNoInteractions(shipments);
    }
    @Test void manualRecipientFieldsAreRequired() {
        for (int i = 0; i < 5; i++) {
            ShipmentRequest r = request();
            switch (i) {
                case 0 -> r.setReceiverName("  "); case 1 -> r.setReceiverSurname(null);
                case 2 -> r.setReceiverCity(""); case 3 -> r.setReceiverDistrict(null);
                case 4 -> r.setReceiverAddressText("  ");
            }
            assertThrows(IllegalArgumentException.class, () -> service.create(r));
        }
        verifyNoInteractions(shipments);
    }
    @Test void manualSenderAddressIsRequired() {
        ShipmentRequest r = request(); r.setSenderAddressText(null);
        assertThrows(IllegalArgumentException.class, () -> service.create(r));
        verifyNoInteractions(shipments);
    }

    Shipment existingShipment() {
        Shipment existing = new Shipment(); existing.setId(42); existing.setSender(user);
        existing.setBarcode("DAY8TEST0001");
        when(shipments.findByIdForUpdate(42)).thenReturn(Optional.of(existing));
        return existing;
    }
    @Test void updateWithoutSenderIdPreservesOwnershipAndBarcode() {
        Shipment existing = existingShipment();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        existing.setCreatedAt(createdAt);
        doReturn(BigDecimal.TEN).when(service).calculatePrice(any(), any(), any(), any());
        when(shipments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var updated = service.update(42, request());
        assertSame(user, existing.getSender());
        assertEquals(user.getId(), updated.getSender().getId());
        assertEquals("DAY8TEST0001", updated.getBarcode());
        assertEquals(ShipmentStatus.CREATED, updated.getStatus());
        assertEquals(createdAt, updated.getCreatedAt());
        assertNull(updated.getDeliveredAt());
    }
    @Test void updateRejectsSenderReplacementWithoutChangingOwnership() {
        Shipment existing = existingShipment();
        ShipmentRequest r = request(); r.setSenderId(8);
        assertThrows(AccessDeniedException.class, () -> service.update(42, r));
        assertSame(user, existing.getSender());
        verify(shipments, never()).save(any());
    }
    @Test void anotherCustomerCannotUpdateShipment() {
        Shipment existing = existingShipment();
        User other = new User(); other.setId(8); existing.setSender(other);
        assertThrows(AccessDeniedException.class, () -> service.update(42, request()));
        assertSame(other, existing.getSender());
        verify(shipments, never()).save(any());
    }
    @Test void previewAndSavedPriceUseSameCalculation() {
        Price price = new Price(); price.setPrice(new BigDecimal("75.00"));
        when(prices.findByMinDesiLessThanEqualAndMaxDesiGreaterThan(any(), any())).thenReturn(price);
        var detail = service.calculatePriceDetail(new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("40"), BigDecimal.ONE);
        assertEquals(new BigDecimal("8.00"), detail.getDesi());
        assertEquals("DESI", detail.getCalculatedBy());
        assertEquals(detail.getPrice(), service.calculatePrice(new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("40"), BigDecimal.ONE));
        var byWeight = service.calculatePriceDetail(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("5"));
        assertEquals("WEIGHT", byWeight.getCalculatedBy());
        assertEquals(new BigDecimal("5"), byWeight.getCalculatedValue());
    }
    @Test void missingPriceIsRejected() {
        assertThrows(EntityNotFoundException.class, () -> service.calculatePriceDetail(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE));
    }
}
