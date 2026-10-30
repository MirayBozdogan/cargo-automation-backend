package com.example.staj1.Dto;

import com.example.staj1.model.ShipmentStatus;
import java.time.Instant;

public record ShipmentTrackingResponse(String barcode, ShipmentStatus status, Instant createdAt, Instant deliveredAt) {
}
