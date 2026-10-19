package com.example.staj1.service;

import com.example.staj1.Dto.AddressRequest;
import com.example.staj1.model.User;
import com.example.staj1.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerAddressAccessTests {
    UserRepository users = mock(UserRepository.class);
    AddressRepository addresses = mock(AddressRepository.class);
    AddressService service = new AddressService(addresses,users,mock(CityRepository.class),mock(DistrictRepository.class));
    User current = new User(), target = new User();
    @BeforeEach void setup() {
        current.setId(7); current.setRole("STAFF"); target.setId(8); target.setRole("USER");
        when(users.findById(8)).thenReturn(Optional.of(target)); when(addresses.findByUserId(8)).thenReturn(List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(current,null,current.getAuthorities()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void staffAndAdminCanReadCustomerAddresses() {
        for (String role : new String[]{"STAFF","ADMIN"}) { current.setRole(role); assertTrue(service.getById(8).isEmpty()); }
    }
    @Test void customerCannotReadAnotherCustomerAddresses() {
        current.setRole("USER"); assertThrows(AccessDeniedException.class, () -> service.getById(8)); verifyNoInteractions(addresses);
    }
    @Test void staffCannotReadOtherStaffOrAdminAddresses() {
        for (String role : new String[]{"STAFF","ADMIN"}) {
            target.setRole(role); assertThrows(AccessDeniedException.class, () -> service.getById(8));
        }
        verifyNoInteractions(addresses);
    }
    @Test void readPermissionDoesNotGrantWritePermission() {
        assertThrows(AccessDeniedException.class, () -> service.create(new AddressRequest(),8));
        assertThrows(AccessDeniedException.class, () -> service.update(8,1,new AddressRequest()));
        assertThrows(AccessDeniedException.class, () -> service.delete(8,1)); verifyNoInteractions(addresses);
    }
    @Test void ownAddressesRemainAvailableToCustomers() {
        current.setRole("USER"); when(addresses.findByUserId(7)).thenReturn(List.of()); assertTrue(service.getById(7).isEmpty());
    }
    @Test void inactiveStaffCannotReadAddresses() {
        current.setActive(false); assertThrows(AccessDeniedException.class, () -> service.getById(8)); verifyNoInteractions(addresses);
    }
}
