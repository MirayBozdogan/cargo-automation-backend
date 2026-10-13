package com.example.staj1.Dto;

import jakarta.validation.constraints.NotNull;

public record StaffActivityRequest(@NotNull(message = "Aktiflik boş olamaz.") Boolean active) {}
