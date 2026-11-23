package com.example.staj1.repository;

import com.example.staj1.Dto.ForgotPasswordRequest;
import com.example.staj1.Dto.ResetPasswordRequest;
import com.example.staj1.model.User;
import com.example.staj1.service.PasswordResetService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Gerçek şemayla (V3) uçtan uca akış; e-posta gönderimi taklit edilir.
@SpringBootTest
@Transactional
class PasswordResetPersistenceTests {
    @Autowired PasswordResetService service;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @PersistenceContext EntityManager em;
    @MockitoBean JavaMailSender mail;
    User user;

    @BeforeEach void setup() {
        user = new User(); user.setEmail(UUID.randomUUID() + "@example.test"); user.setRole("USER"); user.setActive(true);
        user.setName("Deneme"); user.setSurname("Kullanici"); user.setPassword(encoder.encode("eski-sifre-1"));
        user = users.saveAndFlush(user);
    }

    String requestLink() {
        clearInvocations(mail);
        service.requestReset(new ForgotPasswordRequest(user.getEmail()));
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class); verify(mail).send(captor.capture());
        var matcher = Pattern.compile("\\?token=([A-Za-z0-9_-]{43})").matcher(captor.getValue().getText());
        assertTrue(matcher.find()); return matcher.group(1);
    }

    @Test void linkResetsPasswordOnceAndEndsSessions() {
        String token = requestLink();
        service.resetPassword(new ResetPasswordRequest(token, "yeni-sifre-2"));
        em.flush(); em.clear();
        User saved = users.findById(user.getId()).orElseThrow();
        assertTrue(encoder.matches("yeni-sifre-2", saved.getPassword()));
        assertEquals(1, saved.getTokenVersion());
        assertNotNull(tokens.findAll().stream().filter(t -> t.getUser().getId().equals(user.getId())).findFirst().orElseThrow().getUsedAt());
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest(token, "ucuncu-sifre-3")));
    }

    @Test void newerLinkClosesTheEarlierOne() {
        String first = requestLink();
        // Bir dakikalık sınırı aşmak için ilk bağlantının oluşturulma zamanı geriye alınır.
        em.createNativeQuery("update password_reset_tokens set created_at = created_at - interval '2 minutes' where user_id = :id")
                .setParameter("id", user.getId()).executeUpdate();
        String second = requestLink();
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest(first, "yeni-sifre-2")));
        service.resetPassword(new ResetPasswordRequest(second, "yeni-sifre-2"));
    }
}
