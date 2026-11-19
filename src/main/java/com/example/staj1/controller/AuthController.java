package com.example.staj1.controller;

import com.example.staj1.Dto.LoginRequest;
import com.example.staj1.Dto.RegisterRequest;
import com.example.staj1.security.AuthCookies;
import com.example.staj1.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies authCookies;

    public AuthController(AuthService authService, AuthCookies authCookies) {
        this.authService = authService;
        this.authCookies = authCookies;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.ok("Kullanıcı başarıyla kayıt oldu.");
    }

    // JWT yanıt gövdesinde dönmez; yalnız HttpOnly çerez olarak gönderilir.
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request) {

        String token = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookies.session(token).toString())
                .body(Map.of("message", "Giriş başarılı."));
    }

    // HttpOnly çerezi JavaScript silemez; çıkışta sunucu çerezi süresi geçmiş olarak yeniden yazar.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.cleared().toString())
                .build();
    }
}
