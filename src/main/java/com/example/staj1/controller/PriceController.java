package com.example.staj1.controller;

import com.example.staj1.Dto.PriceRequest;
import com.example.staj1.model.Price;
import com.example.staj1.service.PriceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/price")
public class PriceController {
    private final PriceService priceService;

    public PriceController(PriceService priceService) { this.priceService = priceService; }

    @GetMapping
    public List<Price> getAll() { return priceService.getAll(); }

    @PostMapping
    public Price create(@Valid @RequestBody PriceRequest request) { return priceService.create(request); }
}
