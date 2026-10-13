package com.example.staj1.service;

import com.example.staj1.model.User;
import com.example.staj1.repository.*;
import com.example.staj1.specification.UserSpecification;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class UserSearchTests {
    UserRepository users = mock(UserRepository.class);
    UserService service = new UserService(users, mock(AddressRepository.class));
    User current = new User();
    @BeforeEach void setup() {
        current.setId(7); current.setEmail("me@example.test");
        when(users.findByEmail(current.getEmail())).thenReturn(Optional.of(current));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(current.getEmail(), null));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    void verifyScope(String field, Object value) {
        ArgumentCaptor<Specification<User>> captor = ArgumentCaptor.forClass((Class) Specification.class);
        verify(users).findAll(captor.capture());
        Root<User> root = mock(Root.class); CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path path = mock(Path.class); doReturn(path).when(root).get(field);
        captor.getValue().toPredicate(root, mock(CriteriaQuery.class), builder);
        verify(builder).equal(path, value);
    }
    @Test void customerScopeUsesExactIdAndDoesNotMutateFilters() {
        Map<String,String> filters = Map.of(); service.search(filters); verifyScope("id", 7);
        assertTrue(filters.isEmpty());
    }
    @Test void staffOnlySearchesCustomers() { current.setRole("STAFF"); service.search(Map.of()); verifyScope("role", "USER"); }
    @Test void staffCannotAskForAdmins() {
        current.setRole("STAFF"); assertThrows(AccessDeniedException.class, () -> service.search(Map.of("role", "ADMIN")));
        verify(users, never()).findAll(any(Specification.class));
    }
    @Test void unknownAndSensitiveFieldsAreRejected() {
        for (String field : new String[]{"password", "addresses", "authorities", "unknown"})
            assertThrows(IllegalArgumentException.class, () -> UserSpecification.filter(Map.of(field, "value")));
    }
    @Test void typedFiltersRejectInvalidValues() {
        for (Map<String,String> filters : List.of(Map.of("id", "abc"), Map.of("age", "-1"), Map.of("role", "1"), Map.of("active", "yes"), Map.of("name", " ")))
            assertThrows(IllegalArgumentException.class, () -> UserSpecification.filter(filters));
    }
    @Test void roleAndActiveUseTheirOwnTypes() {
        Root<User> root = mock(Root.class); CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path role = mock(Path.class); Path active = mock(Path.class);
        doReturn(role).when(root).get("role"); doReturn(active).when(root).get("active");
        UserSpecification.filter(Map.of("role", "ADMIN", "active", "false")).toPredicate(root, mock(CriteriaQuery.class), builder);
        verify(builder).equal(role, "ADMIN"); verify(builder).equal(active, false);
    }
    @Test void textWildcardCharactersAreLiteral() {
        Root<User> root = mock(Root.class); CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Expression<String> lower = mock(Expression.class); when(builder.lower(any())).thenReturn(lower);
        UserSpecification.filter(Map.of("email", "%_")).toPredicate(root, mock(CriteriaQuery.class), builder);
        verify(builder).like(lower, "%\\%\\_%", '\\');
    }
}
