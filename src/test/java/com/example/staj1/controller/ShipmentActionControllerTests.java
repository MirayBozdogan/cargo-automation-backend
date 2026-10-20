package com.example.staj1.controller;

import com.example.staj1.Dto.ShipmentResponse;
import com.example.staj1.exception.*;
import com.example.staj1.model.ShipmentStatus;
import com.example.staj1.service.ShipmentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShipmentActionControllerTests {
    ShipmentService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        service = mock(ShipmentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ShipmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void actionsReturnNewStatusWithoutRequestBody() throws Exception {
        ShipmentResponse accepted = new ShipmentResponse(); accepted.setStatus(ShipmentStatus.ACCEPTED);
        ShipmentResponse prepared = new ShipmentResponse(); prepared.setStatus(ShipmentStatus.READY_FOR_DISTRIBUTION);
        when(service.accept(42)).thenReturn(accepted); when(service.prepare(42)).thenReturn(prepared);
        mvc.perform(patch("/shipment/42/accept")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(patch("/shipment/42/prepare")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY_FOR_DISTRIBUTION"));
    }
    @Test void invalidStateReturnsConflictMessage() throws Exception {
        when(service.prepare(42)).thenThrow(new ShipmentStateException("Önce kabul edilmeli."));
        mvc.perform(patch("/shipment/42/prepare")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Önce kabul edilmeli."));
    }
    @Test void missingShipmentAndDeniedAccessKeepTheirStatusCodes() throws Exception {
        when(service.accept(42)).thenThrow(new EntityNotFoundException("Gönderi bulunamadı."));
        when(service.prepare(42)).thenThrow(new AccessDeniedException("Yetkiniz yok."));
        mvc.perform(patch("/shipment/42/accept")).andExpect(status().isNotFound());
        mvc.perform(patch("/shipment/42/prepare")).andExpect(status().isForbidden());
    }
}
