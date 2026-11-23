package com.example.staj1.controller;

import com.example.staj1.Dto.ChangePasswordRequest;
import com.example.staj1.security.AuthCookies;
import com.example.staj1.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Oturumdaki kullanıcının kendi hesabıyla ilgili işlemler. Kimlik isteğin gövdesinden değil oturumdan alınır.
@RestController
@RequestMapping("/users/me")
public class AccountController {

    private final AuthService authService;
    private final AuthCookies authCookies;

    public AccountController(AuthService authService, AuthCookies authCookies) {
        this.authService = authService;
        this.authCookies = authCookies;
    }

    // Şifre değişince diğer cihazlardaki oturumlar kapanır; bu cihaz yeni çerezle oturumda kalır.
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                              Authentication authentication) {
        String token = authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookies.session(token).toString())
                .body(Map.of("message", "Şifreniz değiştirildi. Diğer cihazlardaki oturumlar kapatıldı."));
    }
}
