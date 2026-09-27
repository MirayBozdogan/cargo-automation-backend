package com.example.staj1.service;

import com.example.staj1.Dto.LoginRequest;
import com.example.staj1.Dto.RegisterRequest;
import com.example.staj1.model.Customer;
import com.example.staj1.repository.CustomerRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest request) {

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Bu email zaten kayıtlı.");
        }

        if (customerRepository.existsByTc(request.getTc())) {
            throw new IllegalArgumentException("Bu TC kimlik numarası zaten kayıtlı.");
        }

        if (customerRepository.existsByTelNo(request.getTelNo())) {
            throw new IllegalArgumentException("Bu telefon numarası zaten kayıtlı.");
        }

        Customer customer = new Customer();

        customer.setName(request.getName());
        customer.setSurname(request.getSurname());
        customer.setEmail(request.getEmail());
        customer.setAge(request.getAge());
        customer.setTc(request.getTc());
        customer.setTelNo(request.getTelNo());

        customer.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        customer.setRole("USER");

        customerRepository.save(customer);
    }


    public String login(LoginRequest request) {

        Customer customer = customerRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email veya şifre hatalı."
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                customer.getPassword())) {

            throw new IllegalArgumentException(
                    "Email veya şifre hatalı."
            );
        }

        return jwtService.generateToken(customer);
    }
}