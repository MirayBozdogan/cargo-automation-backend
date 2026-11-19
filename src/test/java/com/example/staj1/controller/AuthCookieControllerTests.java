package com.example.staj1.controller;

import com.example.staj1.model.User;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.security.AuthCookies;
import com.example.staj1.service.AuthService;
import com.example.staj1.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AuthCookieControllerTests {
    final UserRepository users = mock(UserRepository.class);
    final JwtService jwt = mock(JwtService.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    MockMvc mvc(AuthCookies cookies) {
        return MockMvcBuilders.standaloneSetup(new AuthController(new AuthService(users, encoder, jwt), cookies)).build();
    }

    @Test void loginSetsHttpOnlySessionCookieAndDoesNotReturnTheToken() throws Exception {
        User user = new User(); user.setEmail("musteri@example.test"); user.setRole("USER");
        user.setPassword(encoder.encode("dogru-sifre-123")); user.setActive(true);
        when(users.findByEmail("musteri@example.test")).thenReturn(Optional.of(user));
        when(jwt.generateToken(any())).thenReturn("signed.jwt.value");

        var response = mvc(new AuthCookies(false, "Strict")).perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"musteri@example.test\",\"password\":\"dogru-sifre-123\"}"))
                .andReturn().getResponse();

        assertEquals(200, response.getStatus());
        String cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.startsWith("cargo_session=signed.jwt.value;"), cookie);
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("Max-Age=86400"));
        assertFalse(cookie.contains("Secure"));
        assertFalse(response.getContentAsString().contains("signed.jwt.value"));
    }

    @Test void secureFlagIsAddedWhenConfigured() throws Exception {
        User user = new User(); user.setEmail("musteri@example.test"); user.setRole("USER");
        user.setPassword(encoder.encode("dogru-sifre-123")); user.setActive(true);
        when(users.findByEmail("musteri@example.test")).thenReturn(Optional.of(user));
        when(jwt.generateToken(any())).thenReturn("signed.jwt.value");
        var response = mvc(new AuthCookies(true, "Lax")).perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"musteri@example.test\",\"password\":\"dogru-sifre-123\"}"))
                .andReturn().getResponse();
        assertTrue(response.getHeader("Set-Cookie").contains("Secure"));
        assertTrue(response.getHeader("Set-Cookie").contains("SameSite=Lax"));
    }

    @Test void failedLoginSetsNoCookie() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        // Standalone kurulumda GlobalExceptionHandler yok; istisna doğrudan yükselir.
        assertThrows(Exception.class, () -> mvc(new AuthCookies(false, "Strict")).perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"yok@example.test\",\"password\":\"yanlis-sifre-123\"}")));
        verify(jwt, never()).generateToken(any());
    }

    @Test void logoutExpiresTheCookie() throws Exception {
        var response = mvc(new AuthCookies(false, "Strict")).perform(post("/auth/logout")).andReturn().getResponse();
        assertEquals(204, response.getStatus());
        assertTrue(response.getHeader("Set-Cookie").startsWith("cargo_session=;"));
        assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=0"));
        assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test void sameSiteNoneIsRejectedBecauseItWouldNeedCsrfTokens() {
        assertThrows(IllegalStateException.class, () -> new AuthCookies(true, "None"));
        assertThrows(IllegalStateException.class, () -> new AuthCookies(false, "strict"));
    }
}
