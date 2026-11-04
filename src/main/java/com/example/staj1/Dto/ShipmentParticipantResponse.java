package com.example.staj1.Dto;

// Gönderi yanıtında hesap bilgileri yerine yalnız ilişki kimliği taşınır.
public class ShipmentParticipantResponse {
    private final Integer id;

    public ShipmentParticipantResponse(Integer id) { this.id = id; }
    public Integer getId() { return id; }
}
