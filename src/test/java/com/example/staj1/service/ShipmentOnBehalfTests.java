package com.example.staj1.service;

import com.example.staj1.Dto.ShipmentRequest;
import com.example.staj1.model.*;
import com.example.staj1.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentOnBehalfTests {
    UserRepository users = mock(UserRepository.class);
    ShipmentRepository shipments = mock(ShipmentRepository.class);
    AddressRepository addresses = mock(AddressRepository.class);
    User operator = new User(), customer = new User();
    ShipmentService service;
    @BeforeEach void setup() {
        operator.setId(7); operator.setRole("STAFF"); operator.setEmail("staff@example.test");
        customer.setId(8); customer.setName("Deneme"); customer.setSurname("Musteri"); customer.setRole("USER");
        when(users.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator)); when(users.findById(8)).thenReturn(Optional.of(customer));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(operator.getEmail(),null,operator.getAuthorities()));
        service = spy(new ShipmentService(shipments,mock(PriceRepository.class),users,addresses));
        doReturn(BigDecimal.TEN).when(service).calculatePrice(any(),any(),any(),any());
        when(shipments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    ShipmentRequest request() {
        ShipmentRequest r = new ShipmentRequest(); r.setSenderId(8);
        r.setWidth(BigDecimal.TEN); r.setLength(BigDecimal.TEN); r.setHeight(BigDecimal.TEN); r.setWeight(BigDecimal.ONE);
        r.setSenderCity("Ankara"); r.setSenderDistrict("Cankaya"); r.setSenderAddressText("Ornek gonderici adresi");
        r.setReceiverName("Alici"); r.setReceiverSurname("Deneme"); r.setReceiverCity("Istanbul"); r.setReceiverDistrict("Kadikoy"); r.setReceiverAddressText("Ornek alici adresi");
        return r;
    }
    @Test void staffAndAdminCanCreateWithCustomerAsSender() {
        for (String role : new String[]{"STAFF","ADMIN"}) {
            operator.setRole(role); var result = service.create(request());
            assertSame(customer,result.getSender()); assertEquals("Deneme",result.getSenderName()); assertEquals(12,result.getBarcode().length());
        }
    }
    @Test void customerCannotImpersonateAnotherCustomer() {
        operator.setRole("USER"); assertThrows(AccessDeniedException.class, () -> service.create(request()));
        verify(users,never()).findById(anyInt()); verify(shipments,never()).save(any());
    }
    @Test void inactiveOperatorCannotCreate() {
        operator.setActive(false); assertThrows(AccessDeniedException.class, () -> service.create(request()));
        verify(shipments,never()).save(any());
    }
    @Test void senderMustBeActiveCustomer() {
        customer.setActive(false); assertThrows(IllegalArgumentException.class, () -> service.create(request()));
        customer.setActive(true);
        for (String role : new String[]{"STAFF","ADMIN"}) {
            customer.setRole(role); assertThrows(IllegalArgumentException.class, () -> service.create(request()));
        }
        verify(shipments,never()).save(any());
    }
    @Test void unknownCustomerIsNotFound() {
        ShipmentRequest r = request(); r.setSenderId(99);
        assertThrows(EntityNotFoundException.class, () -> service.create(r));
    }
    Address address(User owner) {
        City city = new City(1,"Ankara"); District district = new District(1,"Cankaya",city);
        Address address = new Address(); address.setId(15); address.setUser(owner); address.setCity(city); address.setDistrict(district);
        address.setNeighborhood("Ornek Mahalle"); address.setBuildingNo(10); address.setApartmentNo(2); return address;
    }
    ShipmentRequest savedAddressRequest() {
        ShipmentRequest r = request(); r.setSenderAddressId(15); r.setSenderCity(null); r.setSenderDistrict(null); r.setSenderAddressText(null); return r;
    }
    @Test void customerRegisteredAddressIsCopiedIntoShipment() {
        when(addresses.findById(15)).thenReturn(Optional.of(address(customer)));
        var result = service.create(savedAddressRequest());
        assertEquals(8,result.getSender().getId()); assertEquals(15,result.getSenderAddressId());
        assertEquals("Ankara",result.getSenderCity()); assertTrue(result.getSenderAddressText().contains("Ornek Mahalle"));
    }
    @Test void anotherCustomersAddressCannotBeUsed() {
        User another = new User(); another.setId(99);
        when(addresses.findById(15)).thenReturn(Optional.of(address(another)));
        assertThrows(IllegalArgumentException.class, () -> service.create(savedAddressRequest()));
        verify(shipments,never()).save(any());
    }
    @Test void senderNameComesFromCustomerRecord() {
        ShipmentRequest r = request(); r.setSenderName("Forged"); r.setSenderSurname("Name");
        var result = service.create(r); assertEquals("Deneme",result.getSenderName()); assertEquals("Musteri",result.getSenderSurname());
    }
}
