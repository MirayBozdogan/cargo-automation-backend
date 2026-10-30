package com.example.staj1.service;

import com.example.staj1.model.*;
import com.example.staj1.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentTrackingAndCouriersTests {
    UserRepository users; ShipmentRepository shipments; ShipmentService service; User current;
    @BeforeEach void setup() {
        users=mock(UserRepository.class); shipments=mock(ShipmentRepository.class);
        current=new User(); current.setId(7); current.setRole("STAFF");
        when(users.findByEmail("test@example.test")).thenReturn(Optional.of(current));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test@example.test",null));
        service=new ShipmentService(shipments,mock(PriceRepository.class),users,mock(AddressRepository.class));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void courierListUsesActiveStaffQueryAndStablePaging() {
        Pageable pageable=PageRequest.of(1,10,Sort.by("name","surname","id"));
        User courier=new User(); courier.setId(8); courier.setName("Ad"); courier.setSurname("Soyad");
        when(users.findByRoleAndActiveTrue("STAFF",pageable)).thenReturn(new PageImpl<>(List.of(courier),pageable,11));
        for (String role : new String[]{"STAFF","ADMIN"}) {
            current.setRole(role); var result=service.listCouriers(1,10);
            assertEquals(11,result.getTotalElements()); assertEquals(2,result.getTotalPages());
            assertEquals(8,result.getContent().getFirst().id());
        }
    }
    @Test void customerAndInactiveOperatorsCannotListCouriers() {
        current.setRole("USER"); assertThrows(AccessDeniedException.class,()->service.listCouriers(0,10));
        current.setRole("STAFF"); current.setActive(false);
        assertThrows(AccessDeniedException.class,()->service.listCouriers(0,10));
        verify(users,never()).findByRoleAndActiveTrue(any(),any());
    }
    @Test void invalidCourierPaginationIsRejected() {
        for (int[] args : new int[][]{{-1,10},{0,0},{0,51}})
            assertThrows(IllegalArgumentException.class,()->service.listCouriers(args[0],args[1]));
    }
    @Test void trackingNormalizesBarcodeAndQueriesOnlyCurrentUser() {
        Shipment shipment=new Shipment(); shipment.setBarcode("ABCDEF123456"); shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        when(shipments.findTrackingShipment("ABCDEF123456",7)).thenReturn(Optional.of(shipment));
        current.setRole("USER"); var result=service.track(" abcdef123456 ");
        assertEquals("ABCDEF123456",result.barcode()); assertEquals(ShipmentStatus.OUT_FOR_DELIVERY,result.status());
        verify(shipments,never()).findByBarcodeAndDeletedFalse(any());
    }
    @Test void missingDeletedAndUnownedShipmentsHaveSameNotFoundResponse() {
        for (String role : new String[]{"USER","STAFF","ADMIN"}) {
            current.setRole(role);
            var error=assertThrows(EntityNotFoundException.class,()->service.track("ABCDEF123456"));
            assertEquals("Bu barkoda ait erişebileceğiniz gönderi bulunamadı.",error.getMessage());
        }
    }
    @Test void invalidBarcodeOrInactiveAccountIsRejectedBeforeQuery() {
        for (String value : new String[]{null,"","SHORT","ABC/EF123456","ABCDEF1234567"})
            assertThrows(IllegalArgumentException.class,()->service.track(value));
        current.setActive(false); assertThrows(AccessDeniedException.class,()->service.track("ABCDEF123456"));
        verifyNoInteractions(shipments);
    }
}
