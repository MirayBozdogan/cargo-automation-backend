package com.example.staj1.controller;

import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.model.User;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.service.AuthService;
import com.example.staj1.service.JwtService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChangePasswordControllerTests {
    static final String EMAIL = "musteri@example.test";
    UserRepository users; JwtService jwt; MockMvc mvc; BCryptPasswordEncoder encoder; User user;

    @BeforeEach void setup() {
        users = mock(UserRepository.class); jwt = mock(JwtService.class); encoder = new BCryptPasswordEncoder();
        when(jwt.generateToken(any())).thenReturn("new.jwt.value");
        user = new User(); user.setEmail(EMAIL); user.setRole("USER"); user.setActive(true);
        user.setPassword(encoder.encode("eski-sifre-1"));
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        mvc = MockMvcBuilders.standaloneSetup(new AccountController(new AuthService(users, encoder, jwt),
                        new com.example.staj1.security.AuthCookies(false, "Strict")))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    ResultActions change(String current, String next) throws Exception {
        String body = JsonMapper.builder().build().writeValueAsString(Map.of("currentPassword", current, "newPassword", next));
        return mvc.perform(put("/users/me/password").contentType(MediaType.APPLICATION_JSON).content(body)
                .principal(new UsernamePasswordAuthenticationToken(EMAIL, null)));
    }

    @Test void correctCurrentPasswordStoresNewHashAndEndsOtherSessions() throws Exception {
        change("eski-sifre-1", "yeni-sifre-2").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Şifreniz değiştirildi. Diğer cihazlardaki oturumlar kapatıldı."))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.startsWith("cargo_session=new.jwt.value;")));
        var captor = ArgumentCaptor.forClass(User.class); verify(users).save(captor.capture());
        assertEquals(1, captor.getValue().getTokenVersion());
        // Yeni anahtar, sürüm artırıldıktan sonra üretilmeli; yoksa bu cihaz da oturumdan düşer.
        var order = inOrder(users, jwt);
        order.verify(users).save(user); order.verify(jwt).generateToken(user);
        String stored = captor.getValue().getPassword();
        assertNotEquals("yeni-sifre-2", stored);
        assertTrue(encoder.matches("yeni-sifre-2", stored));
        assertFalse(encoder.matches("eski-sifre-1", stored));
    }

    @Test void wrongCurrentPasswordIsRejectedWithoutSaving() throws Exception {
        change("yanlis-sifre", "yeni-sifre-2").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mevcut şifre hatalı."))
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(users, never()).save(any());
        verifyNoInteractions(jwt);
        assertEquals(0, user.getTokenVersion());
    }

    @Test void sameNewPasswordIsRejected() throws Exception {
        change("eski-sifre-1", "eski-sifre-1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Yeni şifre mevcut şifreyle aynı olamaz."));
        verify(users, never()).save(any());
    }

    @Test void shortNewPasswordFailsValidationBeforeDatabaseAccess() throws Exception {
        change("eski-sifre-1", "kisa").andExpect(status().isBadRequest());
        verifyNoInteractions(users);
    }

    @Test void blankCurrentPasswordFailsValidation() throws Exception {
        change("", "yeni-sifre-2").andExpect(status().isBadRequest());
        verifyNoInteractions(users);
    }

    @Test void newPasswordOverBcryptLimitIsRejectedBeforeDatabaseAccess() throws Exception {
        change("eski-sifre-1", "ş".repeat(37)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Şifre UTF-8 biçiminde 72 baytı geçemez."));
        verifyNoInteractions(users);
    }
}
