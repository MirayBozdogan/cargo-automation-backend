package com.example.staj1.service;

import com.example.staj1.Dto.StaffUpdateRequest;
import com.example.staj1.model.User;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.security.JwtAuthenticationFilter;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.*;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffUpdateTests {
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    StaffService service = new StaffService(users, encoder);
    User admin = new User(), staff = new User();
    @BeforeEach void setup() {
        admin.setRole("ADMIN"); admin.setEmail("admin@example.test");
        staff.setId(42); staff.setRole("STAFF"); staff.setEmail("staff@example.test"); staff.setPassword("existing-hash");
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin)); when(users.findById(42)).thenReturn(Optional.of(staff));
        when(users.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getEmail(), null, admin.getAuthorities()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    StaffUpdateRequest request() {
        StaffUpdateRequest r = new StaffUpdateRequest(); r.setName("Yeni"); r.setSurname("Personel"); r.setEmail("updated@example.test");
        r.setAge(30); r.setTc("12345678901"); r.setTelNo("05555554444"); return r;
    }
    @Test void updatesDetailsWithoutChangingRolePasswordOrActivity() {
        staff.setActive(false); var response = service.update(42, request());
        assertEquals("Yeni", response.name()); assertEquals("updated@example.test", response.email());
        assertEquals("STAFF", response.role()); assertFalse(response.active()); assertEquals("existing-hash", staff.getPassword());
        verifyNoInteractions(encoder);
    }
    @Test void canDeactivateAndReactivateStaff() {
        assertFalse(service.setActive(42, false).active()); assertTrue(service.setActive(42, true).active());
        assertEquals("existing-hash", staff.getPassword()); assertEquals("STAFF", staff.getRole());
    }
    @Test void customerOrAdminTargetIsRejected() {
        for (String role : new String[]{"USER", "ADMIN"}) {
            staff.setRole(role);
            assertThrows(IllegalArgumentException.class, () -> service.update(42, request()));
            assertThrows(IllegalArgumentException.class, () -> service.setActive(42, false));
        }
        verify(users, never()).saveAndFlush(any());
    }
    @Test void nonAdminsCannotUpdateOrDeactivateStaff() {
        for (String role : new String[]{"USER", "STAFF"}) {
            admin.setRole(role);
            assertThrows(AccessDeniedException.class, () -> service.update(42, request()));
            assertThrows(AccessDeniedException.class, () -> service.setActive(42, false));
        }
        verify(users, never()).saveAndFlush(any());
    }
    @Test void inactiveAdminIsRejected() {
        admin.setActive(false); assertThrows(AccessDeniedException.class, () -> service.setActive(42, false));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void missingStaffIs404Condition() {
        assertThrows(EntityNotFoundException.class, () -> service.setActive(99, false));
    }
    @Test void duplicateContactDetailsAreRejectedBeforeMutation() {
        when(users.existsByEmailAndIdNot("updated@example.test",42)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.update(42, request()));
        assertEquals("staff@example.test", staff.getEmail()); verify(users, never()).saveAndFlush(any());
    }
    @Test void unchangedUniqueFieldsAreAllowedWithOwnIdExcluded() {
        service.update(42, request());
        verify(users).existsByEmailAndIdNot("updated@example.test", 42);
        verify(users).existsByTcAndIdNot("12345678901", 42); verify(users).existsByTelNoAndIdNot("05555554444", 42);
    }
    @Test void deactivationImmediatelyRejectsExistingToken() throws Exception {
        service.setActive(42, false); SecurityContextHolder.clearContext();
        JwtService jwt = mock(JwtService.class); when(jwt.extractUsername("existing-token")).thenReturn(staff.getEmail());
        when(users.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer existing-token");
        MockHttpServletResponse response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
        new JwtAuthenticationFilter(jwt, users).doFilter(request,response,chain);
        assertEquals(401,response.getStatus()); verifyNoInteractions(chain);
    }
}
