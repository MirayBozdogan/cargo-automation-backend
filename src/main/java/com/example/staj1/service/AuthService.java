package com.example.staj1.service;

import com.example.staj1.Dto.LoginRequest;
import com.example.staj1.Dto.RegisterRequest;
import com.example.staj1.model.User;
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

    public void register(RegisterRequest request) {

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

        user.setRole("USER");

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

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new IllegalArgumentException(
                    "Email veya şifre hatalı."
            );
        }

        return jwtService.generateToken(user);
    }
}