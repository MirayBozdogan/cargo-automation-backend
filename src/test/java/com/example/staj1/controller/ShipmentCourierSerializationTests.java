package com.example.staj1.controller;

import com.example.staj1.Dto.CourierResponse;
import com.example.staj1.Dto.ShipmentResponse;
import com.example.staj1.model.Shipment;
import com.example.staj1.model.User;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class ShipmentCourierSerializationTests {
    JsonMapper mapper = JsonMapper.builder().build();
    @Test void rawShipmentNeverSerializesCourierAccount() {
        User courier = new User(); courier.setId(8); courier.setEmail("private@example.test");
        courier.setPassword("secret"); courier.setTc("11111111111");
        Shipment shipment = new Shipment(); shipment.setCourier(courier);
        var json = mapper.readTree(mapper.writeValueAsString(shipment));
        assertFalse(json.has("courier"));
        String output = json.toString();
        assertFalse(output.contains("private@example.test")); assertFalse(output.contains("11111111111"));
        assertFalse(output.contains("secret"));
    }
    @Test void customerResponseOnlyIncludesCourierIdentitySummary() {
        ShipmentResponse response = new ShipmentResponse(); response.setCourier(new CourierResponse(8,"Ad","Soyad"));
        var json = mapper.readTree(mapper.writeValueAsString(response)).get("courier");
        assertEquals(3,json.size()); assertEquals(8,json.get("id").asInt());
        assertEquals("Ad",json.get("name").asText()); assertEquals("Soyad",json.get("surname").asText());
    }
}
