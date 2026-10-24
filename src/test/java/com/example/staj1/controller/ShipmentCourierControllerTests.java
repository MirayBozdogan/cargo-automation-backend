package com.example.staj1.controller;

import com.example.staj1.Dto.*;
import com.example.staj1.exception.*;
import com.example.staj1.model.ShipmentStatus;
import com.example.staj1.service.ShipmentService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShipmentCourierControllerTests {
    ShipmentService service;
    MockMvc mvc;
    ShipmentOperationDetailResponse response(ShipmentStatus status, Instant deliveredAt) {
        return new ShipmentOperationDetailResponse(42,"TEST00000042",status,null,null,null,null,null,
                null,null,null,null,null,null,null,null,null,null,null,deliveredAt,new CourierResponse(8,"Kurye","Deneme"));
    }
    @BeforeEach void setup() {
        service=mock(ShipmentService.class);
        mvc=MockMvcBuilders.standaloneSetup(new ShipmentController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void assignmentUsesCourierIdAndReturnsSafeCourierSummary() throws Exception {
        when(service.assignCourier(42,8)).thenReturn(response(ShipmentStatus.OUT_FOR_DELIVERY,null));
        mvc.perform(patch("/shipment/42/assign-courier").contentType(MediaType.APPLICATION_JSON).content("{\"courierId\":8}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"))
                .andExpect(jsonPath("$.courier.id").value(8)).andExpect(jsonPath("$.courier.name").value("Kurye"))
                .andExpect(jsonPath("$.courier.email").doesNotExist()).andExpect(jsonPath("$.courier.password").doesNotExist())
                .andExpect(jsonPath("$.courier.tc").doesNotExist());
    }
    @Test void invalidCourierInputNeverReachesService() throws Exception {
        for (String body : new String[]{"{}","{\"courierId\":null}","{\"courierId\":0}","{\"courierId\":-1}",
                "{\"courierId\":\"8\"}","{\"courierId\":1.5}"}) {
            mvc.perform(patch("/shipment/42/assign-courier").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void deliveryNeedsNoBodyAndReturnsDeliveredStatusAndDate() throws Exception {
        when(service.deliver(42)).thenReturn(response(ShipmentStatus.DELIVERED,Instant.parse("2026-01-02T10:00:00Z")));
        mvc.perform(patch("/shipment/42/deliver")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED")).andExpect(jsonPath("$.deliveredAt").exists());
    }
    @Test void invalidStateUsesConflictResponse() throws Exception {
        when(service.deliver(42)).thenThrow(new ShipmentStateException("Gönderi dağıtımda değil."));
        mvc.perform(patch("/shipment/42/deliver")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Gönderi dağıtımda değil."));
    }
}
