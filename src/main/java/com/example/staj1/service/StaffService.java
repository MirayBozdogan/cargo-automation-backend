package com.example.staj1.service;

import com.example.staj1.Dto.StaffRequest;
import com.example.staj1.Dto.StaffUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class StaffService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public StaffService(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> list(Pageable pageable) {
        requireAdmin();
        if (pageable.getPageSize() > 50) {
            throw new IllegalArgumentException("Sayfa boyutu en fazla 50 olabilir.");
        }
        Set<String> sortable = Set.of("id", "name", "surname", "email", "active");
        for (var order : pageable.getSort()) {
            if (!sortable.contains(order.getProperty())) {
                throw new IllegalArgumentException("Desteklenmeyen sıralama alanı.");
            }
        }
        return users.findByRole(UserRole.STAFF.name(), pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public StaffResponse getById(Integer id) {
        requireAdmin();
        return response(findStaff(id));
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        requireAdmin();
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
        return response(save(staff));
    }

    @Transactional
    public StaffResponse update(Integer id, StaffUpdateRequest request) {
        requireAdmin();
        User staff = findStaff(id);
        String email = request.getEmail().trim();
        if (users.existsByEmailAndIdNot(email, id)) throw new IllegalArgumentException("Bu e-posta başka bir hesapta kullanılıyor.");
        if (users.existsByTcAndIdNot(request.getTc(), id)) throw new IllegalArgumentException("Bu TC başka bir hesapta kullanılıyor.");
        if (users.existsByTelNoAndIdNot(request.getTelNo(), id)) throw new IllegalArgumentException("Bu telefon başka bir hesapta kullanılıyor.");
        staff.setName(request.getName().trim()); staff.setSurname(request.getSurname().trim());
        staff.setEmail(email); staff.setAge(request.getAge()); staff.setTc(request.getTc()); staff.setTelNo(request.getTelNo());
        return response(save(staff));
    }

    @Transactional
    public StaffResponse setActive(Integer id, boolean active) {
        requireAdmin();
        User staff = findStaff(id);
        staff.setActive(active);
        return response(save(staff));
    }

    private void requireAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Personel yönetmek için yönetici olmalısınız.");
        }
        User admin = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Yönetici hesabı bulunamadı."));
        if (!admin.isActive() || !UserRole.ADMIN.name().equals(admin.getRole())) {
            throw new AccessDeniedException("Personel yönetmek için aktif yönetici olmalısınız.");
        }
    }

    private User findStaff(Integer id) {
        User staff = users.findById(id).orElseThrow(() -> new EntityNotFoundException("Personel bulunamadı."));
        if (!UserRole.STAFF.name().equals(staff.getRole())) {
            throw new IllegalArgumentException("Bu işlem yalnız personel hesaplarına uygulanabilir.");
        }
        return staff;
    }

    private User save(User staff) {
        try { return users.saveAndFlush(staff); }
        catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("E-posta, TC veya telefon başka bir hesapta kullanılıyor.");
        }
    }

    private StaffResponse response(User staff) {
        return new StaffResponse(staff.getId(), staff.getName(), staff.getSurname(), staff.getEmail(),
                staff.getAge(), staff.getTc(), staff.getTelNo(), staff.getRole(), staff.isActive());
    }
}
