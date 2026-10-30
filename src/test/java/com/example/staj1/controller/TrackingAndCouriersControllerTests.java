package com.example.staj1.controller;

import com.example.staj1.Dto.*;
import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.model.ShipmentStatus;
import com.example.staj1.service.ShipmentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TrackingAndCouriersControllerTests {
    ShipmentService service; MockMvc mvc;
    @BeforeEach void setup() {
        service=mock(ShipmentService.class);
        mvc=MockMvcBuilders.standaloneSetup(new ShipmentController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void courierListExposesOnlyIdentitySummaryAndPaging() throws Exception {
        when(service.listCouriers(0,10)).thenReturn(new PageImpl<>(List.of(new CourierResponse(8,"Ad","Soyad")),PageRequest.of(0,10),1));
        mvc.perform(get("/shipment/couriers")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(8)).andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].tc").doesNotExist()).andExpect(jsonPath("$.content[0].password").doesNotExist());
    }
    @Test void courierPaginationIsPassedToService() throws Exception {
        when(service.listCouriers(1,5)).thenReturn(Page.empty(PageRequest.of(1,5)));
        mvc.perform(get("/shipment/couriers").param("page","1").param("size","5")).andExpect(status().isOk());
        verify(service).listCouriers(1,5);
    }
    @Test void trackingExposesStatusAndDatesWithoutPersonalDetails() throws Exception {
        when(service.track("ABCDEF123456")).thenReturn(new ShipmentTrackingResponse("ABCDEF123456",ShipmentStatus.DELIVERED,
                Instant.parse("2026-01-01T10:00:00Z"),Instant.parse("2026-01-02T10:00:00Z")));
        mvc.perform(get("/shipment/tracking/ABCDEF123456")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED")).andExpect(jsonPath("$.deliveredAt").exists())
                .andExpect(jsonPath("$.sender").doesNotExist()).andExpect(jsonPath("$.senderAddressText").doesNotExist())
                .andExpect(jsonPath("$.receiver").doesNotExist()).andExpect(jsonPath("$.courier").doesNotExist());
    }
    @Test void trackingNotFoundIsReportedWithoutDisclosure() throws Exception {
        when(service.track("ABCDEF123456")).thenThrow(new EntityNotFoundException("Gönderi bulunamadı."));
        mvc.perform(get("/shipment/tracking/ABCDEF123456")).andExpect(status().isNotFound());
    }
}
