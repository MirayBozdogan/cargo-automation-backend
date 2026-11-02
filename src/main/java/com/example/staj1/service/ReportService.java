package com.example.staj1.service;

import com.example.staj1.Dto.ShipmentReportResponse;
import com.example.staj1.model.ShipmentStatus;
import com.example.staj1.model.User;
import com.example.staj1.model.UserRole;
import com.example.staj1.repository.ShipmentRepository;
import com.example.staj1.repository.UserRepository;
import java.util.Collections;
import java.util.EnumMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    public ReportService(ShipmentRepository shipmentRepository, UserRepository userRepository) {
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ShipmentReportResponse getShipmentReport() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Rapor için yönetici girişi gereklidir.");
        }
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Kullanıcı bulunamadı."));
        if (!currentUser.isActive() || !UserRole.ADMIN.name().equals(currentUser.getRole())) {
            throw new AccessDeniedException("Raporu yalnız aktif yöneticiler görebilir.");
        }

        var statusCounts = new EnumMap<ShipmentStatus, Long>(ShipmentStatus.class);
        for (ShipmentStatus status : ShipmentStatus.values()) statusCounts.put(status, 0L);
        // Tek sorgudan hesaplanır: toplam ve durum sayıları aynı veri anını temsil eder.
        for (var row : shipmentRepository.countByStatus()) statusCounts.put(row.status(), row.count());
        long totalShipments = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        return new ShipmentReportResponse(totalShipments, statusCounts.get(ShipmentStatus.DELIVERED),
                Collections.unmodifiableMap(statusCounts));
    }
}
