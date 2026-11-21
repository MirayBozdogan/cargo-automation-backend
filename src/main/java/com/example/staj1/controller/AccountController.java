package com.example.staj1.controller;

import com.example.staj1.Dto.ChangePasswordRequest;
import com.example.staj1.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Oturumdaki kullanıcının kendi hesabıyla ilgili işlemler. Kimlik isteğin gövdesinden değil oturumdan alınır.
@RestController
@RequestMapping("/users/me")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                              Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return Map.of("message", "Şifreniz değiştirildi.");
    }
}
