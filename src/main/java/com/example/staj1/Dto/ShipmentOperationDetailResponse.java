package com.example.staj1.Dto;

import com.example.staj1.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ShipmentOperationDetailResponse(
        Integer id, String barcode, ShipmentStatus status,
        BigDecimal width, BigDecimal length, BigDecimal height, BigDecimal weight, BigDecimal price,
        String senderName, String senderSurname, String senderCity, String senderDistrict, String senderAddressText,
        String receiverName, String receiverSurname, String receiverCity, String receiverDistrict, String receiverAddressText,
        Instant createdAt, Instant deliveredAt) {
}
