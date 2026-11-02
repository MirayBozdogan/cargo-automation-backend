package com.example.staj1.Dto;

import com.example.staj1.model.ShipmentStatus;
import java.util.Map;

public record ShipmentReportResponse(long totalShipments, long deliveredShipments,
        Map<ShipmentStatus, Long> statusCounts) {}
