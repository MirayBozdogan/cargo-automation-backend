package com.example.staj1.Dto;

import com.example.staj1.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ShipmentSummaryResponse(
        Integer id, String barcode, ShipmentStatus status,
        String senderName, String senderSurname,
        String receiverName, String receiverSurname,
        BigDecimal price, Instant createdAt) {
}
