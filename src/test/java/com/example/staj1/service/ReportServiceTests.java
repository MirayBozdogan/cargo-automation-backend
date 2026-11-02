package com.example.staj1.service;

import com.example.staj1.Dto.ShipmentStatusCount;
import com.example.staj1.model.*;
import com.example.staj1.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportServiceTests {
    ShipmentRepository shipments; UserRepository users; ReportService service; User admin;
    @BeforeEach void setup() {
        shipments = mock(ShipmentRepository.class); users = mock(UserRepository.class);
        service = new ReportService(shipments, users);
        admin = new User(); admin.setRole("ADMIN"); admin.setEmail("admin@example.test");
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin.getEmail(), null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void emptyReportIncludesEveryStatusWithZero() {
        when(shipments.countByStatus()).thenReturn(List.of());
        var report = service.getShipmentReport();
        assertEquals(0, report.totalShipments()); assertEquals(0, report.deliveredShipments());
        assertEquals(ShipmentStatus.values().length, report.statusCounts().size());
        assertTrue(report.statusCounts().values().stream().allMatch(count -> count == 0));
    }
    @Test void totalAndDeliveredAreDerivedFromOneGroupedQueryWithLongCounts() {
        when(shipments.countByStatus()).thenReturn(List.of(
                new ShipmentStatusCount(ShipmentStatus.CREATED, 3_000_000_000L),
                new ShipmentStatusCount(ShipmentStatus.DELIVERED, 4L)));
        var report = service.getShipmentReport();
        assertEquals(3_000_000_004L, report.totalShipments()); assertEquals(4L, report.deliveredShipments());
        assertEquals(0L, report.statusCounts().get(ShipmentStatus.ACCEPTED));
        verify(shipments, times(1)).countByStatus(); verifyNoMoreInteractions(shipments);
    }
    @Test void nonAdminsAndInactiveAdminsCannotReadCounts() {
        for (String role : new String[]{"USER", "STAFF"}) {
            admin.setRole(role); assertThrows(AccessDeniedException.class, service::getShipmentReport);
        }
        admin.setRole("ADMIN"); admin.setActive(false);
        assertThrows(AccessDeniedException.class, service::getShipmentReport);
        verifyNoInteractions(shipments);
    }
    @Test void missingSessionAndDeletedAccountCannotReadCounts() {
        when(users.findByEmail(admin.getEmail())).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, service::getShipmentReport);
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, service::getShipmentReport);
        verifyNoInteractions(shipments);
    }
}
