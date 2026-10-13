package com.example.staj1.security;

import com.example.staj1.config.DevAdminInitializer;
import com.example.staj1.Dto.LoginRequest;
import com.example.staj1.model.User;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.service.AuthService;
import com.example.staj1.service.JwtService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountFoundationTests {
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    JwtService jwt = mock(JwtService.class);
    String email = "admin@example.test";
    String password = "TestOnlyPassword!2026";
    @BeforeEach void setup() { when(jdbc.queryForObject("SELECT current_database()", String.class)).thenReturn("cargo_automation_dev"); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    DevAdminInitializer initializer() { return new DevAdminInitializer(users, encoder, jdbc, email, password); }
    @Test void newUserIsActiveAndRolesMapToAuthorities() {
        User user = new User(); assertTrue(user.isEnabled()); assertTrue(user.isActive());
        for (String role : new String[]{"USER", "STAFF", "ADMIN"}) {
            user.setRole(role); assertEquals("ROLE_" + role, user.getAuthorities().iterator().next().getAuthority());
        }
        assertThrows(IllegalArgumentException.class, () -> user.setRole("UNKNOWN"));
    }
    @Test void inactiveAccountCannotLogin() {
        User user = new User(); user.setActive(false);
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        LoginRequest request = new LoginRequest(); request.setEmail(email); request.setPassword(password);
        assertThrows(IllegalArgumentException.class, () -> new AuthService(users, encoder, jwt).login(request));
        verifyNoInteractions(encoder, jwt);
    }
    @Test void inactiveAccountCannotUseExistingToken() throws Exception {
        User user = new User(); user.setActive(false);
        when(jwt.extractUsername("test-token")).thenReturn(email);
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer test-token");
        MockHttpServletResponse response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
        new JwtAuthenticationFilter(jwt, users).doFilter(request, response, chain);
        assertEquals(401, response.getStatus()); verifyNoInteractions(chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void adminIsCreatedWithEncodedPassword() {
        when(users.findByEmail(email)).thenReturn(Optional.empty()); when(encoder.encode(password)).thenReturn("encoded");
        initializer().run();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class); verify(users).save(captor.capture());
        User admin = captor.getValue(); assertEquals("ADMIN", admin.getRole()); assertTrue(admin.isEnabled());
        assertEquals("encoded", admin.getPassword());
    }
    @Test void existingAdminIsNotResetOrReactivated() {
        User existing = new User(); existing.setRole("ADMIN"); existing.setActive(false);
        when(users.findByEmail(email)).thenReturn(Optional.of(existing)); initializer().run();
        verify(users, never()).save(any()); verifyNoInteractions(encoder); assertFalse(existing.isActive());
    }
    @Test void existingCustomerIsNeverPromoted() {
        when(users.findByEmail(email)).thenReturn(Optional.of(new User()));
        assertThrows(IllegalStateException.class, () -> initializer().run());
        verify(users, never()).save(any());
    }
    @Test void refusesOtherDatabases() {
        when(jdbc.queryForObject("SELECT current_database()", String.class)).thenReturn("staj1");
        assertThrows(IllegalStateException.class, () -> initializer().run()); verifyNoInteractions(users, encoder);
    }
    @Test void invalidAdminCredentialsDoNotCreateAccount() {
        assertThrows(IllegalStateException.class, () -> new DevAdminInitializer(users, encoder, jdbc, email, "short").run());
        assertThrows(IllegalStateException.class, () -> new DevAdminInitializer(users, encoder, jdbc, "invalid", password).run());
        verifyNoInteractions(users, encoder);
    }
}
