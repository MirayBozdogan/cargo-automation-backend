package com.example.staj1.controller;

import com.example.staj1.Dto.ShipmentReportResponse;
import com.example.staj1.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/shipments")
    public ShipmentReportResponse getShipmentReport() {
        return reportService.getShipmentReport();
    }
}
