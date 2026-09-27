package com.example.staj1.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 saat

    // JWT oluşturur
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public String extractUsername(String token) {
        return getAllClaims(token).getSubject();
    }


    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    // Token'ın süresi dolmuş mu?
    private boolean isTokenExpired(String token) {
        return getAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    // Token içindeki bilgileri alır
    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody(); //JWT'nin payload kısmındaki bilgileri (Claims) alıyor.
    }

    // Secret key'i güvenlik anahtarına çevirir
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes); // Keys sınıfının hazır statik metodudur.
        // Kendisine verilen byte dizisini alır ve HMAC-SHA şifreleme algoritmalarında (örneğin HS256)
        // kullanılmak üzere resmi bir Key nesnesine dönüştürür.
    }
}