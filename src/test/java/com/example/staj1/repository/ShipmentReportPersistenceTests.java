package com.example.staj1.repository;

import com.example.staj1.model.*;
import com.example.staj1.service.ReportService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ShipmentReportPersistenceTests {
    @Autowired ShipmentRepository shipments;
    @Autowired UserRepository users;
    @Autowired ReportService reports;
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    void add(ShipmentStatus status, boolean deleted) {
        Shipment shipment = new Shipment(); shipment.setStatus(status); shipment.setDeleted(deleted);
        shipment.setBarcode(UUID.randomUUID().toString().replace("-", "").substring(0,12));
        shipments.saveAndFlush(shipment);
    }
    @Test void groupedReportMatchesStoredStatusesAndExcludesSoftDeletedRecords() {
        User admin = new User(); admin.setEmail(UUID.randomUUID()+"@example.test"); admin.setRole("ADMIN");
        users.saveAndFlush(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin.getEmail(), null, List.of()));
        var before = reports.getShipmentReport();
        for (ShipmentStatus status : ShipmentStatus.values()) { add(status, false); add(status, true); }
        add(ShipmentStatus.DELIVERED, false);
        var after = reports.getShipmentReport();
        assertEquals(before.totalShipments()+6, after.totalShipments());
        assertEquals(before.deliveredShipments()+2, after.deliveredShipments());
        for (ShipmentStatus status : ShipmentStatus.values()) {
            long added = status == ShipmentStatus.DELIVERED ? 2 : 1;
            assertEquals(before.statusCounts().get(status)+added, after.statusCounts().get(status));
        }
        assertEquals(after.totalShipments(), after.statusCounts().values().stream().mapToLong(Long::longValue).sum());
    }
}
