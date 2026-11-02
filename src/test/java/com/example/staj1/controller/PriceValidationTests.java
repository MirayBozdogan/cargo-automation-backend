package com.example.staj1.controller;

import com.example.staj1.Dto.PriceRequest;
import com.example.staj1.exception.*;
import com.example.staj1.model.Price;
import com.example.staj1.service.PriceService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PriceValidationTests {
    PriceService service; MockMvc mvc;
    @BeforeEach void setup() {
        service = mock(PriceService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PriceController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void validRequestUsesServiceInsteadOfDirectRepositorySave() throws Exception {
        Price price = new Price(); price.setId(42); when(service.create(any())).thenReturn(price);
        mvc.perform(post("/price").contentType(MediaType.APPLICATION_JSON)
                .content("{\"minDesi\":0,\"maxDesi\":1,\"price\":60}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42));
        verify(service).create(argThat(request -> request.getMinDesi().signum() == 0 && request.getPrice().intValue() == 60));
    }
    @Test void missingNegativeZeroAndOverpreciseValuesAreRejectedBeforeService() throws Exception {
        for (String json : new String[]{"{}", "{\"minDesi\":-1,\"maxDesi\":1,\"price\":60}",
                "{\"minDesi\":0,\"maxDesi\":0,\"price\":60}", "{\"minDesi\":0,\"maxDesi\":1,\"price\":0}",
                "{\"minDesi\":0,\"maxDesi\":1,\"price\":1.001}", "{\"minDesi\":0.001,\"maxDesi\":1,\"price\":60}",
                "{\"minDesi\":0,\"maxDesi\":1.001,\"price\":60}"}) {
            mvc.perform(post("/price").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        }
        verifyNoInteractions(service);
    }
    @Test void overlapReturns409AndReversedBoundsReturn400() throws Exception {
        when(service.create(any(PriceRequest.class))).thenThrow(new PriceRangeConflictException("Tarife çakışıyor."));
        mvc.perform(post("/price").contentType(MediaType.APPLICATION_JSON)
                .content("{\"minDesi\":0,\"maxDesi\":1,\"price\":60}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Tarife çakışıyor."));
        when(service.create(any(PriceRequest.class))).thenThrow(new IllegalArgumentException("Geçersiz aralık."));
        mvc.perform(post("/price").contentType(MediaType.APPLICATION_JSON)
                .content("{\"minDesi\":2,\"maxDesi\":1,\"price\":60}"))
                .andExpect(status().isBadRequest());
    }
}
