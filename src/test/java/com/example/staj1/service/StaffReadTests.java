package com.example.staj1.service;

import com.example.staj1.model.User;
import com.example.staj1.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffReadTests {
    UserRepository users = mock(UserRepository.class);
    StaffService service = new StaffService(users, mock(PasswordEncoder.class));
    User admin = new User();
    @BeforeEach void setup() {
        admin.setRole("ADMIN"); admin.setEmail("admin@example.test");
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getEmail(), null, admin.getAuthorities()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    User staff(int id, boolean active) {
        User user = new User(); user.setId(id); user.setRole("STAFF"); user.setActive(active); user.setName("Deneme"); user.setSurname("Personel"); return user;
    }
    @Test void listIsPagedAndIncludesActiveAndInactiveStaff() {
        var pageable = PageRequest.of(0, 10, Sort.by("id").descending());
        when(users.findByRole("STAFF", pageable)).thenReturn(new PageImpl<>(List.of(staff(2,true),staff(1,false)),pageable,12));
        var result = service.list(pageable);
        assertEquals(12,result.getTotalElements()); assertEquals(2,result.getTotalPages());
        assertTrue(result.getContent().get(0).active()); assertFalse(result.getContent().get(1).active());
        verify(users).findByRole("STAFF",pageable);
    }
    @Test void emptyListIsSuccessful() {
        var pageable = PageRequest.of(0,10); when(users.findByRole("STAFF",pageable)).thenReturn(Page.empty(pageable));
        assertTrue(service.list(pageable).isEmpty());
    }
    @Test void nonAdminsCannotReadStaff() {
        for (String role : new String[]{"USER","STAFF"}) {
            admin.setRole(role);
            assertThrows(AccessDeniedException.class, () -> service.list(PageRequest.of(0,10)));
            assertThrows(AccessDeniedException.class, () -> service.getById(42));
        }
        verify(users,never()).findByRole(anyString(),any()); verify(users,never()).findById(anyInt());
    }
    @Test void inactiveAdminCannotReadStaff() {
        admin.setActive(false); assertThrows(AccessDeniedException.class, () -> service.list(PageRequest.of(0,10)));
    }
    @Test void listRejectsOversizedPagesAndSensitiveSort() {
        assertThrows(IllegalArgumentException.class, () -> service.list(PageRequest.of(0,51)));
        assertThrows(IllegalArgumentException.class, () -> service.list(PageRequest.of(0,10,Sort.by("password"))));
        verify(users,never()).findByRole(anyString(),any());
    }
    @Test void detailReturnsStaffOnly() {
        when(users.findById(42)).thenReturn(Optional.of(staff(42,true)));
        assertEquals(42,service.getById(42).id());
        when(users.findById(43)).thenReturn(Optional.of(admin));
        assertThrows(IllegalArgumentException.class, () -> service.getById(43));
    }
    @Test void missingStaffIsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> service.getById(99));
    }
}
