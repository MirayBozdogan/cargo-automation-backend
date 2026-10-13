package com.example.staj1.service;

import com.example.staj1.Dto.StaffRequest;
import com.example.staj1.Dto.StaffResponse;
import com.example.staj1.model.User;
import com.example.staj1.model.UserRole;
import com.example.staj1.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

@Service
public class StaffService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public StaffService(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Personel eklemek için yönetici olmalısınız.");
        }
        User admin = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Yönetici hesabı bulunamadı."));
        if (!admin.isActive() || !UserRole.ADMIN.name().equals(admin.getRole())) {
            throw new AccessDeniedException("Personel eklemek için aktif yönetici olmalısınız.");
        }
        String email = request.getEmail().trim();
        if (users.existsByEmail(email)) throw new IllegalArgumentException("Bu e-posta zaten kayıtlı.");
        if (users.existsByTc(request.getTc())) throw new IllegalArgumentException("Bu TC kimlik numarası zaten kayıtlı.");
        if (users.existsByTelNo(request.getTelNo())) throw new IllegalArgumentException("Bu telefon zaten kayıtlı.");
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Şifre UTF-8 biçiminde 72 baytı geçemez.");
        }
        User staff = new User();
        staff.setName(request.getName().trim()); staff.setSurname(request.getSurname().trim());
        staff.setEmail(email); staff.setAge(request.getAge()); staff.setTc(request.getTc()); staff.setTelNo(request.getTelNo());
        staff.setPassword(encoder.encode(request.getPassword()));
        staff.setRole(UserRole.STAFF.name()); staff.setActive(true);
        try {
            staff = users.saveAndFlush(staff);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("E-posta, TC veya telefon başka bir hesapta kullanılıyor.");
        }
        return new StaffResponse(staff.getId(), staff.getName(), staff.getSurname(), staff.getEmail(),
                staff.getAge(), staff.getTc(), staff.getTelNo(), staff.getRole(), staff.isActive());
    }
}
