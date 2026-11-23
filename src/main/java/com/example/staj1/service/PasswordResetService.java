package com.example.staj1.service;

import com.example.staj1.Dto.ForgotPasswordRequest;
import com.example.staj1.Dto.ResetPasswordRequest;
import com.example.staj1.model.PasswordResetToken;
import com.example.staj1.model.User;
import com.example.staj1.repository.PasswordResetTokenRepository;
import com.example.staj1.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    // Aynı hesaba art arda bağlantı gönderilmesini sınırlar.
    static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);
    static final String INVALID_LINK = "Bağlantı geçersiz veya süresi dolmuş. Lütfen yeni bir bağlantı isteyin.";

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final Clock clock;
    private final String resetUrl;
    private final String mailFrom;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder passwordEncoder, JavaMailSender mailSender,
                                @Value("${app.password-reset.url}") String resetUrl,
                                @Value("${app.mail.from}") String mailFrom) {
        this(users, tokens, passwordEncoder, mailSender, Clock.systemUTC(), resetUrl, mailFrom);
    }

    PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                         PasswordEncoder passwordEncoder, JavaMailSender mailSender, Clock clock,
                         String resetUrl, String mailFrom) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.clock = clock;
        this.resetUrl = resetUrl;
        this.mailFrom = mailFrom;
    }

    // Hesap olsun olmasın aynı yanıt döner; böylece bu uç noktayla hangi e-postaların kayıtlı olduğu öğrenilemez.
    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        User user = users.findByEmail(request.getEmail().trim()).orElse(null);
        if (user == null || !user.isEnabled()) {
            return;
        }
        Instant now = clock.instant();
        if (tokens.existsByUserIdAndCreatedAtAfter(user.getId(), now.minus(RESEND_INTERVAL))) {
            return;
        }

        tokens.expireUnusedForUser(user.getId(), now);
        String token = newToken();
        tokens.save(new PasswordResetToken(user, hash(token), now, now.plus(TOKEN_LIFETIME)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Kargo Otomasyonu şifre sıfırlama");
        message.setText("Merhaba " + user.getName() + ",\n\n"
                + "Şifreni sıfırlamak için aşağıdaki bağlantıyı aç. Bağlantı "
                + TOKEN_LIFETIME.toMinutes() + " dakika geçerlidir ve yalnız bir kez kullanılabilir.\n\n"
                + resetUrl + "?token=" + token + "\n\n"
                + "Bu isteği sen yapmadıysan bu e-postayı yok sayabilirsin; şifren değişmez.\n");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // Yanıt aynı kalır; bağlantı ve e-posta adresi log'a yazılmaz.
            log.warn("Şifre sıfırlama e-postası gönderilemedi (kullanıcı id {}): {}", user.getId(), ex.getMessage());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getNewPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Şifre UTF-8 biçiminde 72 baytı geçemez.");
        }
        Instant now = clock.instant();
        PasswordResetToken token = tokens.findByTokenHash(hash(request.getToken()))
                .filter(found -> found.getUsedAt() == null && found.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_LINK));
        User user = token.getUser();
        if (!user.isEnabled() || tokens.markUsed(token.getId(), now) != 1) {
            throw new IllegalArgumentException(INVALID_LINK);
        }

        user = users.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException(INVALID_LINK));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Şifresini unutan kişinin hesabı başkasının elinde olabilir; açık bütün oturumlar kapanır.
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);
        tokens.expireUnusedForUser(user.getId(), now);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
