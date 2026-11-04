package com.example.staj1.controller;

import com.example.staj1.model.*;
import com.example.staj1.repository.*;
import com.example.staj1.service.ShipmentService;
import com.example.staj1.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ShipmentCustomerResponseTests {
    ShipmentRepository shipments; MockMvc mvc; User sender, receiver; Shipment shipment;
    JsonMapper mapper = JsonMapper.builder().build();
    @BeforeEach void setup() {
        shipments = mock(ShipmentRepository.class); UserRepository users = mock(UserRepository.class);
        sender = account(7); receiver = account(8);
        when(users.findByEmail("sender@example.test")).thenReturn(Optional.of(sender));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("sender@example.test", null));
        shipment = new Shipment(); shipment.setId(42); shipment.setBarcode("ABCDEF123456");
        shipment.setSender(sender); shipment.setReceiver(receiver); shipment.setSenderName("Gönderi anındaki isim");
        shipment.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        shipment.setSenderAddressText("Çıkış adresi"); shipment.setReceiverAddressText("Teslimat adresi");
        when(shipments.findByIdAndDeletedFalse(42)).thenReturn(Optional.of(shipment));
        when(shipments.findByBarcodeAndDeletedFalse(shipment.getBarcode())).thenReturn(Optional.of(shipment));
        when(shipments.findBySender_IdAndDeletedFalseOrReceiver_IdAndDeletedFalse(eq(7), eq(7), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(shipment), call.getArgument(2), 1));
        var service = new ShipmentService(shipments, mock(PriceRepository.class), users, mock(AddressRepository.class));
        mvc = MockMvcBuilders.standaloneSetup(new ShipmentController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    User account(int id) {
        User user = new User(); user.setId(id); user.setEmail("private"+id+"@example.test");
        user.setTc("11111111111"); user.setTelNo("05555555555"); user.setPassword("private-password");
        user.setRole("ADMIN"); return user;
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void listDetailAndLegacyBarcodeHaveTheSameSafeResponseShape() throws Exception {
        var detailResult = mvc.perform(get("/shipment/42")).andReturn();
        var barcodeResult = mvc.perform(get("/shipment/barcode/ABCDEF123456")).andReturn();
        var pageResult = mvc.perform(get("/shipment?page=0&size=5")).andReturn();
        assertEquals(200, detailResult.getResponse().getStatus());
        assertEquals(200, barcodeResult.getResponse().getStatus()); assertEquals(200, pageResult.getResponse().getStatus());
        var detail = mapper.readTree(detailResult.getResponse().getContentAsString());
        var barcode = mapper.readTree(barcodeResult.getResponse().getContentAsString());
        var page = mapper.readTree(pageResult.getResponse().getContentAsString());
        assertEquals(detail, barcode); assertEquals(detail, page.get("content").get(0));
        assertEquals(1, page.get("totalElements").asInt()); assertEquals(5, page.get("size").asInt());
        assertEquals(1, detail.get("sender").size()); assertEquals(7, detail.get("sender").get("id").asInt());
        assertEquals(1, detail.get("receiver").size()); assertEquals(8, detail.get("receiver").get("id").asInt());
        assertEquals("Gönderi anındaki isim", detail.get("senderName").asText());
        assertEquals("Çıkış adresi", detail.get("senderAddressText").asText());
        for (String forbidden : new String[]{"email", "password", "tc", "telNo", "authorities", "accountNonLocked", "active", "role", "deleted"}) {
            assertFalse(detail.has(forbidden), forbidden);
            assertFalse(detail.get("sender").has(forbidden), forbidden);
            assertFalse(detail.get("receiver").has(forbidden), forbidden);
        }
    }
    @Test void manualRecipientIsNullInsteadOfAnEmptyAccountObject() throws Exception {
        shipment.setReceiver(null);
        var result = mvc.perform(get("/shipment/42")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        var json = mapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("receiver").isNull()); assertEquals(7, json.get("sender").get("id").asInt());
    }
    @Test void foreignShipmentStillReturnsForbiddenForIdAndBarcode() throws Exception {
        shipment.setSender(receiver); shipment.setReceiver(null);
        assertEquals(403, mvc.perform(get("/shipment/42")).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(get("/shipment/barcode/ABCDEF123456")).andReturn().getResponse().getStatus());
    }
}
