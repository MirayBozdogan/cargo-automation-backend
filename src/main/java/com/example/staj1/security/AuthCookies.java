package com.example.staj1.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT'yi JavaScript'in okuyamadığı HttpOnly bir çerezde taşır.
 * SameSite yalnız Strict veya Lax olabilir: başka sitelerden gönderilen isteklere çerez eklenmez,
 * bu da çerezli oturumda CSRF saldırılarına karşı ana korumadır.
 */
@Component
public class AuthCookies {

    public static final String NAME = "cargo_session";
    public static final Duration MAX_AGE = Duration.ofHours(24); // JwtService ile aynı süre

    private final boolean secure;
    private final String sameSite;

    public AuthCookies(
            @Value("${app.auth.cookie-secure:false}") boolean secure,
            @Value("${app.auth.cookie-same-site:Strict}") String sameSite) {
        if (!"Strict".equals(sameSite) && !"Lax".equals(sameSite)) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE yalnız Strict veya Lax olabilir.");
        }
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie session(String token) {
        return base(token).maxAge(MAX_AGE).build();
    }

    public ResponseCookie cleared() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) return cookie.getValue();
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
