package com.example.staj1.controller;

import com.example.staj1.Dto.ShipmentSummaryResponse;
import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.model.ShipmentStatus;
import com.example.staj1.service.ShipmentService;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShipmentOperationsControllerTests {
    ShipmentService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        service = mock(ShipmentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ShipmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void listReturnsSummaryWithoutNestedUserOrAddressData() throws Exception {
        var summary = new ShipmentSummaryResponse(42,"TEST00000042",ShipmentStatus.CREATED,
                "Ad","Soyad","Alıcı","Soyad",BigDecimal.TEN,Instant.parse("2026-01-01T10:00:00Z"));
        when(service.getOperations(null,0,10)).thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0,10), 1));
        mvc.perform(get("/shipment/operations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].barcode").value("TEST00000042"))
                .andExpect(jsonPath("$.content[0].status").value("CREATED"))
                .andExpect(jsonPath("$.content[0].sender").doesNotExist())
                .andExpect(jsonPath("$.content[0].receiver").doesNotExist())
                .andExpect(jsonPath("$.content[0].senderAddressText").doesNotExist());
    }
    @Test void controllerPassesFilterAndPageToService() throws Exception {
        when(service.getOperations(ShipmentStatus.ACCEPTED,1,5)).thenReturn(Page.empty(PageRequest.of(1,5)));
        mvc.perform(get("/shipment/operations").param("status","ACCEPTED").param("page","1").param("size","5"))
                .andExpect(status().isOk());
        verify(service).getOperations(ShipmentStatus.ACCEPTED,1,5);
    }
    @Test void invalidStatusAndInvalidPaginationReturnBadRequest() throws Exception {
        mvc.perform(get("/shipment/operations").param("status","UNKNOWN")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        when(service.getOperations(null,-1,10)).thenThrow(new IllegalArgumentException("Geçersiz sayfa."));
        mvc.perform(get("/shipment/operations").param("page","-1")).andExpect(status().isBadRequest());
    }
}
