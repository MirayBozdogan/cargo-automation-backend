package com.example.staj1.controller;

import com.example.staj1.Dto.StaffRequest;
import com.example.staj1.Dto.StaffUpdateRequest;
import com.example.staj1.Dto.StaffActivityRequest;
import com.example.staj1.Dto.StaffResponse;
import com.example.staj1.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff")
public class StaffController {
    private final StaffService staffService;

    public StaffController(StaffService staffService) { this.staffService = staffService; }

    @GetMapping
    public Page<StaffResponse> list(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return staffService.list(pageable);
    }

    @GetMapping("/{id}")
    public StaffResponse getById(@PathVariable Integer id) {
        return staffService.getById(id);
    }

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(request));
    }
    @PutMapping("/{id}")
    public StaffResponse update(@PathVariable Integer id, @Valid @RequestBody StaffUpdateRequest request) {
        return staffService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public StaffResponse setActive(@PathVariable Integer id, @Valid @RequestBody StaffActivityRequest request) {
        return staffService.setActive(id, request.active());
    }
}
