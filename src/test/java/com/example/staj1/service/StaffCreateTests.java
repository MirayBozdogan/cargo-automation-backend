package com.example.staj1.service;

import com.example.staj1.Dto.*;
import com.example.staj1.model.User;
import com.example.staj1.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.Validation;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffCreateTests {
    UserRepository users = mock(UserRepository.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    StaffService service = new StaffService(users, encoder);
    User admin = new User();
    @BeforeEach void setup() {
        admin.setRole("ADMIN"); admin.setEmail("admin@example.test");
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getEmail(), null, admin.getAuthorities()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    StaffRequest request() {
        StaffRequest r = new StaffRequest(); r.setName("Deneme"); r.setSurname("Personel"); r.setEmail("staff@example.test");
        r.setAge(25); r.setTc("12345678901"); r.setTelNo("05555554444"); r.setPassword("TestOnlyPassword!2026"); return r;
    }
    @Test void createsActiveStaffWithHashedPasswordAndSafeResponse() {
        when(users.saveAndFlush(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0); saved.setId(42); return saved;
        });
        StaffRequest request = request(); StaffResponse response = service.create(request);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class); verify(users).saveAndFlush(captor.capture());
        User stored = captor.getValue();
        assertEquals("STAFF", stored.getRole()); assertTrue(stored.isActive());
        assertNotEquals(request.getPassword(), stored.getPassword()); assertTrue(encoder.matches(request.getPassword(), stored.getPassword()));
        assertEquals(42, response.id()); assertEquals("STAFF", response.role());
        assertTrue(java.util.Arrays.stream(StaffResponse.class.getRecordComponents()).noneMatch(c -> c.getName().equals("password")));
    }
    @Test void customersAndStaffCannotCreateStaff() {
        for (String role : new String[]{"USER", "STAFF"}) {
            admin.setRole(role); assertThrows(AccessDeniedException.class, () -> service.create(request()));
        }
        verify(users, never()).saveAndFlush(any());
    }
    @Test void inactiveAdminCannotCreateStaff() {
        admin.setActive(false); assertThrows(AccessDeniedException.class, () -> service.create(request()));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void missingSessionIsRejected() {
        SecurityContextHolder.clearContext(); assertThrows(AccessDeniedException.class, () -> service.create(request()));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void duplicateEmailIsRejected() {
        when(users.existsByEmail("staff@example.test")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(request())); verify(users, never()).saveAndFlush(any());
    }
    @Test void duplicateIdentityIsRejected() {
        when(users.existsByTc("12345678901")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(request())); verify(users, never()).saveAndFlush(any());
    }
    @Test void duplicatePhoneIsRejected() {
        when(users.existsByTelNo("05555554444")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(request())); verify(users, never()).saveAndFlush(any());
    }
    @Test void concurrentDuplicateGetsControlledError() {
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThrows(IllegalArgumentException.class, () -> service.create(request()));
    }
    @Test void passwordIsLimitedByUtf8Bytes() {
        StaffRequest r = request(); r.setPassword("ğ".repeat(40));
        assertThrows(IllegalArgumentException.class, () -> service.create(r)); verify(users, never()).saveAndFlush(any());
    }
    @Test void invalidFieldsAreRejectedByValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator(); assertTrue(validator.validate(request()).isEmpty());
            StaffRequest r = request(); r.setEmail("invalid"); r.setAge(17); r.setTc("abcdefghijk"); r.setTelNo("123"); r.setPassword("short");
            assertEquals(5, validator.validate(r).size());
        }
    }
}
