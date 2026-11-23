package com.example.staj1.service;

import com.example.staj1.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTests {
    static final String SECRET = Base64.getEncoder().encodeToString("test-anahtari-en-az-otuz-iki-bayt-uzunlukta".getBytes());
    JwtService jwt; User user;

    @BeforeEach void setup() {
        jwt = new JwtService(); ReflectionTestUtils.setField(jwt, "secretKey", SECRET);
        user = new User(); user.setEmail("musteri@example.test"); user.setRole("USER");
    }

    @Test void tokenIsValidForCurrentVersion() {
        user.setTokenVersion(3);
        assertTrue(jwt.isTokenValid(jwt.generateToken(user), user));
    }

    @Test void raisingVersionInvalidatesEarlierTokens() {
        String before = jwt.generateToken(user);
        user.setTokenVersion(1);
        assertFalse(jwt.isTokenValid(before, user));
        assertTrue(jwt.isTokenValid(jwt.generateToken(user), user));
    }

    @Test void tokenWithoutVersionClaimCountsAsVersionZero() {
        // Bu sürümden önce verilmiş anahtarlar "ver" alanı taşımaz.
        String legacy = Jwts.builder().setSubject(user.getEmail()).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)), SignatureAlgorithm.HS256).compact();
        assertTrue(jwt.isTokenValid(legacy, user));
        user.setTokenVersion(1);
        assertFalse(jwt.isTokenValid(legacy, user));
    }

    @Test void tokenForAnotherEmailIsRejected() {
        String token = jwt.generateToken(user);
        User other = new User(); other.setEmail("baska@example.test"); other.setRole("USER");
        assertFalse(jwt.isTokenValid(token, other));
    }
}
