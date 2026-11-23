package com.example.staj1.service;

import com.example.staj1.model.User;
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
    static final String TOKEN_VERSION_CLAIM = "ver";

    // JWT oluşturur
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim(TOKEN_VERSION_CLAIM, tokenVersion(userDetails))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public String extractUsername(String token) {
        return getAllClaims(token).getSubject();
    }


    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = getAllClaims(token);
        // Sayı taşımayan eski anahtarlar 0 kabul edilir; hesap şifresini değiştirene kadar geçerli kalırlar.
        Number version = claims.get(TOKEN_VERSION_CLAIM, Number.class);

        return claims.getSubject().equals(userDetails.getUsername())
                && (version == null ? 0 : version.intValue()) == tokenVersion(userDetails)
                && !claims.getExpiration().before(new Date());
    }

    private static int tokenVersion(UserDetails userDetails) {
        return userDetails instanceof User user ? user.getTokenVersion() : 0;
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