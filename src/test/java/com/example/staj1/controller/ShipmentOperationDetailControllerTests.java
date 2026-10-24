package com.example.staj1.controller;

import com.example.staj1.Dto.ShipmentOperationDetailResponse;
import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.model.ShipmentStatus;
import com.example.staj1.service.ShipmentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShipmentOperationDetailControllerTests {
    ShipmentService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        service=mock(ShipmentService.class);
        mvc=MockMvcBuilders.standaloneSetup(new ShipmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void detailIncludesSnapshotAndMeasurementsWithoutAccountObjects() throws Exception {
        var detail=new ShipmentOperationDetailResponse(42,"TEST00000042",ShipmentStatus.CREATED,
                BigDecimal.TEN,BigDecimal.TEN,BigDecimal.TEN,BigDecimal.ONE,BigDecimal.TEN,
                "Ad","Soyad","Ankara","Çankaya","Çıkış adresi",
                "Alıcı","Soyad","İstanbul","Kadıköy","Teslimat adresi",Instant.parse("2026-01-01T10:00:00Z"),null);
        when(service.getOperationDetail(42)).thenReturn(detail);
        mvc.perform(get("/shipment/operations/42")).andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("TEST00000042"))
                .andExpect(jsonPath("$.senderAddressText").value("Çıkış adresi"))
                .andExpect(jsonPath("$.receiverAddressText").value("Teslimat adresi"))
                .andExpect(jsonPath("$.weight").value(1))
                .andExpect(jsonPath("$.sender").doesNotExist())
                .andExpect(jsonPath("$.receiver").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.tc").doesNotExist());
    }
    @Test void missingAndDeniedDetailUseCorrectHttpStatus() throws Exception {
        when(service.getOperationDetail(42)).thenThrow(new EntityNotFoundException("Gönderi bulunamadı."));
        when(service.getOperationDetail(43)).thenThrow(new AccessDeniedException("Yetkiniz yok."));
        mvc.perform(get("/shipment/operations/42")).andExpect(status().isNotFound());
        mvc.perform(get("/shipment/operations/43")).andExpect(status().isForbidden());
    }
    @Test void invalidIdDoesNotBecomeServerError() throws Exception {
        mvc.perform(get("/shipment/operations/abc")).andExpect(status().isBadRequest());
        when(service.getOperationDetail(0)).thenThrow(new IllegalArgumentException("Geçersiz gönderi numarası."));
        mvc.perform(get("/shipment/operations/0")).andExpect(status().isBadRequest());
    }
}
