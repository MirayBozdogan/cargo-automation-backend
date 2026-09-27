package com.example.staj1.controller;

import com.example.staj1.Dto.CalculatePriceResponse;
import com.example.staj1.Dto.ShipmentRequest;
import com.example.staj1.Dto.ShipmentResponse;
import com.example.staj1.model.Shipment;
import com.example.staj1.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/shipment")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }


    @GetMapping
    public Page<Shipment> getAll(Pageable pageable) {
        return shipmentService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public ShipmentResponse getById(@PathVariable Integer id) {

        return shipmentService.getById(id);
    }

    @GetMapping("/barcode/{barcode}")
    public Shipment getByBarcode(@PathVariable String barcode) {
        return shipmentService.getByBarcode(barcode);
    }

    @PostMapping
    public ShipmentResponse create(
            @Valid @RequestBody ShipmentRequest shipmentRequest) {

        return shipmentService.create(shipmentRequest);
    }

    @PutMapping("/{id}")
    public ShipmentResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody ShipmentRequest request) {

        return shipmentService.update(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {

        shipmentService.delete(id);

        return ResponseEntity.noContent().build();
    }
    @PostMapping("/calculate-price")
    public CalculatePriceResponse calculatePrice(
            @Valid @RequestBody ShipmentRequest request) {

        return shipmentService.calculatePriceDetail(
                request.getWidth(),
                request.getLength(),
                request.getHeight(),
                request.getWeight()
        );
    }



}
