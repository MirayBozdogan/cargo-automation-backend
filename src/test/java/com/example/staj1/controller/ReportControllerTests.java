package com.example.staj1.controller;

import com.example.staj1.Dto.ShipmentReportResponse;
import com.example.staj1.model.ShipmentStatus;
import com.example.staj1.service.ReportService;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportControllerTests {
    @Test void reportSerializesOnlyAggregateCountsWithAllStatusKeys() throws Exception {
        ReportService service = mock(ReportService.class);
        var counts = new EnumMap<ShipmentStatus, Long>(ShipmentStatus.class);
        for (var status : ShipmentStatus.values()) counts.put(status, 0L);
        counts.put(ShipmentStatus.CREATED, 3L); counts.put(ShipmentStatus.DELIVERED, 2L);
        when(service.getShipmentReport()).thenReturn(new ShipmentReportResponse(5, 2, counts));
        MockMvcBuilders.standaloneSetup(new ReportController(service)).build()
                .perform(get("/reports/shipments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalShipments").value(5))
                .andExpect(jsonPath("$.deliveredShipments").value(2))
                .andExpect(jsonPath("$.statusCounts.CREATED").value(3))
                .andExpect(jsonPath("$.statusCounts.ACCEPTED").value(0))
                .andExpect(jsonPath("$.statusCounts.READY_FOR_DISTRIBUTION").value(0))
                .andExpect(jsonPath("$.statusCounts.OUT_FOR_DELIVERY").value(0))
                .andExpect(jsonPath("$.statusCounts.DELIVERED").value(2))
                .andExpect(jsonPath("$.shipments").doesNotExist());
    }
}
