package com.example.staj1.service;

import com.example.staj1.model.User;
import com.example.staj1.repository.*;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class CustomerSearchTests {
    UserRepository users = mock(UserRepository.class);
    UserService service = new UserService(users,mock(AddressRepository.class));
    User current = new User(); Pageable pageable = PageRequest.of(0,10,Sort.by("id").descending());
    @BeforeEach void setup() {
        current.setRole("STAFF"); current.setEmail("staff@example.test");
        when(users.findByEmail(current.getEmail())).thenReturn(Optional.of(current));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(current.getEmail(),null,current.getAuthorities()));
        when(users.findAll(any(Specification.class),eq(pageable))).thenReturn(Page.empty(pageable));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void staffAndAdminCanReadCustomerPages() {
        for (String role : new String[]{"STAFF","ADMIN"}) {
            current.setRole(role); assertTrue(service.searchCustomers(Map.of(),pageable).isEmpty());
        }
    }
    @Test void customersAndInactiveEmployeesAreRejected() {
        current.setRole("USER"); assertThrows(AccessDeniedException.class, () -> service.searchCustomers(Map.of(),pageable));
        current.setRole("STAFF"); current.setActive(false); assertThrows(AccessDeniedException.class, () -> service.searchCustomers(Map.of(),pageable));
        verify(users,never()).findAll(any(Specification.class),any(Pageable.class));
    }
    @Test void queryAlwaysHasExactCustomerRoleScope() {
        service.searchCustomers(Map.of(),pageable);
        ArgumentCaptor<Specification<User>> captor = ArgumentCaptor.forClass((Class) Specification.class);
        verify(users).findAll(captor.capture(),eq(pageable));
        Root<User> root = mock(Root.class); CriteriaBuilder builder = mock(CriteriaBuilder.class); Path role = mock(Path.class);
        doReturn(role).when(root).get("role");
        captor.getValue().toPredicate(root,mock(CriteriaQuery.class),builder);
        verify(builder).equal(role,"USER");
    }
    @Test void rejectsRoleOverrideUnknownFiltersAndBadTypes() {
        for (Map<String,String> filters : List.of(Map.of("role","ADMIN"),Map.of("password","x"),Map.of("active","yes"),Map.of("age","abc"))) {
            assertThrows(IllegalArgumentException.class, () -> service.searchCustomers(filters,pageable));
        }
        verify(users,never()).findAll(any(Specification.class),any(Pageable.class));
    }
    @Test void rejectsLargePagesAndSensitiveSortFields() {
        assertThrows(IllegalArgumentException.class, () -> service.searchCustomers(Map.of(),PageRequest.of(0,51)));
        assertThrows(IllegalArgumentException.class, () -> service.searchCustomers(Map.of(),PageRequest.of(0,10,Sort.by("password"))));
    }
    @Test void mapsPageToCustomerResponseAndPreservesMetadata() {
        User customer = new User(); customer.setId(42); customer.setName("Deneme"); customer.setPassword("secret-hash"); customer.setTc("12345678901");
        when(users.findAll(any(Specification.class),eq(pageable))).thenReturn(new PageImpl<>(List.of(customer),pageable,21));
        var result = service.searchCustomers(Map.of("name","Deneme"),pageable);
        assertEquals(21,result.getTotalElements()); assertEquals(3,result.getTotalPages()); assertEquals(42,result.getContent().get(0).id());
        assertTrue(Arrays.stream(result.getContent().get(0).getClass().getRecordComponents()).noneMatch(c -> Set.of("password","tc","addresses").contains(c.getName())));
    }
}
