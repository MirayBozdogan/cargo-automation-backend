package com.example.staj1.controller;

import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.service.PasswordResetService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordResetControllerTests {
    PasswordResetService service; MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(PasswordResetService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PasswordResetController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void forgotPasswordAlwaysAnswersTheSame() throws Exception {
        mvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"yok@example.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bu e-posta adresiyle kayıtlı bir hesap varsa şifre sıfırlama bağlantısı gönderildi."));
    }

    @Test void invalidEmailFailsValidation() throws Exception {
        mvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"eposta-degil\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void invalidLinkMessageIsReturned() throws Exception {
        doThrow(new IllegalArgumentException("Bağlantı geçersiz veya süresi dolmuş. Lütfen yeni bir bağlantı isteyin."))
                .when(service).resetPassword(any());
        mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"x\",\"newPassword\":\"yeni-sifre-2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bağlantı geçersiz veya süresi dolmuş. Lütfen yeni bir bağlantı isteyin."));
    }

    @Test void shortNewPasswordFailsValidation() throws Exception {
        mvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"x\",\"newPassword\":\"kisa\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
