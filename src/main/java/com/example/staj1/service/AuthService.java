package com.example.staj1.service;

import com.example.staj1.Dto.ChangePasswordRequest;
import com.example.staj1.Dto.LoginRequest;
import com.example.staj1.Dto.RegisterRequest;
import com.example.staj1.model.User;
import com.example.staj1.model.UserRole;
import com.example.staj1.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // BCrypt 72 bayttan sonrasını yok sayar; daha uzun şifre sessizce kısaltılmış gibi davranırdı.
    private static void requireBcryptLength(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Şifre UTF-8 biçiminde 72 baytı geçemez.");
        }
    }

    public void register(RegisterRequest request) {
        requireBcryptLength(request.getPassword());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Bu email zaten kayıtlı.");
        }

        if (userRepository.existsByTc(request.getTc())) {
            throw new IllegalArgumentException("Bu TC kimlik numarası zaten kayıtlı.");
        }

        if (userRepository.existsByTelNo(request.getTelNo())) {
            throw new IllegalArgumentException("Bu telefon numarası zaten kayıtlı.");
        }

        User user = new User();

        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        user.setTc(request.getTc());
        user.setTelNo(request.getTelNo());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(UserRole.USER.name());

        userRepository.save(user);
    }


    public String login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email veya şifre hatalı."
                        )
                );

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Bu hesap pasif. Giriş yapamazsınız.");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new IllegalArgumentException(
                    "Email veya şifre hatalı."
            );
        }

        return jwtService.generateToken(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        requireBcryptLength(request.getNewPassword());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mevcut şifre hatalı.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Yeni şifre mevcut şifreyle aynı olamaz.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
