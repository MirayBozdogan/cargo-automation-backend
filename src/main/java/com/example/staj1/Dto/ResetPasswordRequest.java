package com.example.staj1.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Bağlantı geçersiz veya süresi dolmuş.")
    private String token;

    @NotBlank(message = "Yeni şifre boş olamaz.")
    @Size(min = 6, message = "Yeni şifre en az 6 karakter olmalıdır.")
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
