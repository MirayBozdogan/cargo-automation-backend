package com.example.staj1.controller;

import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.model.User;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.service.AuthService;
import com.example.staj1.service.JwtService;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistrationPasswordTests {
    UserRepository users; MockMvc mvc; BCryptPasswordEncoder encoder;
    @BeforeEach void setup() {
        users = mock(UserRepository.class); encoder = new BCryptPasswordEncoder();
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(new AuthService(users, encoder, mock(JwtService.class))))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    String body(String password) {
        return JsonMapper.builder().build().writeValueAsString(Map.of("name","Test","surname","Deneme",
                "email","test@example.test","age",25,"tc","99999999999","telNo","05999999999","password",password));
    }
    void accepted(String password) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(password)))
                .andExpect(status().isOk());
        var captor = ArgumentCaptor.forClass(User.class); verify(users).save(captor.capture());
        assertEquals("USER", captor.getValue().getRole());
        assertNotEquals(password, captor.getValue().getPassword());
        assertTrue(encoder.matches(password,captor.getValue().getPassword()));
    }
    @Test void seventyTwoAsciiBytesAreAcceptedAndHashed() throws Exception { accepted("a".repeat(72)); }
    @Test void seventyTwoUtf8BytesAreAcceptedAndHashed() throws Exception { accepted("ş".repeat(36)); }
    @Test void overLimitAsciiPasswordIsRejectedWithClearMessageBeforeDatabaseAccess() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("a".repeat(73))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Şifre UTF-8 biçiminde 72 baytı geçemez."));
        verifyNoInteractions(users);
    }
    @Test void utf8ByteLimitIsAppliedInsteadOfCharacterCount() throws Exception {
        for (String password : new String[]{"ş".repeat(37), "a".repeat(71)+"ş"}) {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(password)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(users);
    }
}
