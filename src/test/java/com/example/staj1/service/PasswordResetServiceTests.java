package com.example.staj1.service;

import com.example.staj1.Dto.ForgotPasswordRequest;
import com.example.staj1.Dto.ResetPasswordRequest;
import com.example.staj1.model.PasswordResetToken;
import com.example.staj1.model.User;
import com.example.staj1.repository.PasswordResetTokenRepository;
import com.example.staj1.repository.UserRepository;
import java.time.*;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTests {
    static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    static final String URL = "http://localhost:3000/reset-password";
    UserRepository users; PasswordResetTokenRepository tokens; JavaMailSender mail;
    BCryptPasswordEncoder encoder; PasswordResetService service; User user;

    @BeforeEach void setup() {
        users = mock(UserRepository.class); tokens = mock(PasswordResetTokenRepository.class);
        mail = mock(JavaMailSender.class); encoder = new BCryptPasswordEncoder();
        service = new PasswordResetService(users, tokens, encoder, mail, Clock.fixed(NOW, ZoneOffset.UTC), URL, "no-reply@example.test");
        user = new User(); user.setId(7); user.setName("Ayşe"); user.setEmail("ayse@example.test"); user.setRole("USER");
        user.setActive(true); user.setPassword(encoder.encode("eski-sifre-1"));
        when(users.findByEmail("ayse@example.test")).thenReturn(Optional.of(user));
        when(users.findById(7)).thenReturn(Optional.of(user));
    }

    String sentToken() {
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class); verify(mail).send(captor.capture());
        var matcher = Pattern.compile(Pattern.quote(URL) + "\\?token=([A-Za-z0-9_-]{43})").matcher(captor.getValue().getText());
        assertTrue(matcher.find(), captor.getValue().getText());
        return matcher.group(1);
    }

    PasswordResetToken stored(Instant expiresAt, Instant usedAt) {
        PasswordResetToken token = new PasswordResetToken(user, PasswordResetService.hash("baglanti"), NOW.minusSeconds(60), expiresAt);
        ReflectionTestUtils.setField(token, "id", 3L); ReflectionTestUtils.setField(token, "usedAt", usedAt);
        when(tokens.findByTokenHash(PasswordResetService.hash("baglanti"))).thenReturn(Optional.of(token));
        return token;
    }

    @Test void activeAccountGetsOneTimeLinkAndOnlyHashIsStored() {
        service.requestReset(new ForgotPasswordRequest(" ayse@example.test "));
        var saved = ArgumentCaptor.forClass(PasswordResetToken.class); verify(tokens).save(saved.capture());
        String token = sentToken();
        assertEquals(PasswordResetService.hash(token), saved.getValue().getTokenHash());
        assertNotEquals(token, saved.getValue().getTokenHash());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), saved.getValue().getExpiresAt());
        verify(tokens).expireUnusedForUser(7, NOW);
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class); verify(mail).send(message.capture());
        assertArrayEquals(new String[]{"ayse@example.test"}, message.getValue().getTo());
    }

    @Test void unknownOrInactiveAccountSendsNothing() {
        service.requestReset(new ForgotPasswordRequest("yok@example.test"));
        user.setActive(false);
        service.requestReset(new ForgotPasswordRequest("ayse@example.test"));
        verify(tokens, never()).save(any()); verifyNoInteractions(mail);
    }

    @Test void secondRequestWithinAMinuteSendsNothing() {
        when(tokens.existsByUserIdAndCreatedAtAfter(7, NOW.minus(Duration.ofMinutes(1)))).thenReturn(true);
        service.requestReset(new ForgotPasswordRequest("ayse@example.test"));
        verify(tokens, never()).save(any()); verifyNoInteractions(mail);
    }

    @Test void mailFailureDoesNotChangeTheResponse() {
        doThrow(new MailSendException("bağlantı yok")).when(mail).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.requestReset(new ForgotPasswordRequest("ayse@example.test")));
    }

    @Test void validLinkSetsPasswordEndsSessionsAndIsUsedOnce() {
        stored(NOW.plusSeconds(600), null);
        when(tokens.markUsed(3L, NOW)).thenReturn(1);
        service.resetPassword(new ResetPasswordRequest("baglanti", "yeni-sifre-2"));
        assertTrue(encoder.matches("yeni-sifre-2", user.getPassword()));
        assertEquals(1, user.getTokenVersion());
        verify(users).save(user); verify(tokens).markUsed(3L, NOW); verify(tokens).expireUnusedForUser(7, NOW);
    }

    @Test void expiredUsedOrUnknownLinksAreRejectedWithoutChangingPassword() {
        String before = user.getPassword();
        stored(NOW, null);
        assertEquals(PasswordResetService.INVALID_LINK, assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(new ResetPasswordRequest("baglanti", "yeni-sifre-2"))).getMessage());
        stored(NOW.plusSeconds(600), NOW.minusSeconds(5));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest("baglanti", "yeni-sifre-2")));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest("baska", "yeni-sifre-2")));
        assertEquals(before, user.getPassword()); verify(users, never()).save(any()); verify(tokens, never()).markUsed(any(), any());
    }

    @Test void linkUsedConcurrentlyByAnotherRequestIsRejected() {
        stored(NOW.plusSeconds(600), null);
        when(tokens.markUsed(3L, NOW)).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest("baglanti", "yeni-sifre-2")));
        verify(users, never()).save(any());
    }

    @Test void inactiveAccountCannotUseLink() {
        stored(NOW.plusSeconds(600), null); user.setActive(false);
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest("baglanti", "yeni-sifre-2")));
        verify(tokens, never()).markUsed(any(), any()); verify(users, never()).save(any());
    }

    @Test void newPasswordOverBcryptLimitIsRejectedBeforeLookup() {
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(new ResetPasswordRequest("baglanti", "ş".repeat(37))));
        verifyNoInteractions(tokens);
    }
}
