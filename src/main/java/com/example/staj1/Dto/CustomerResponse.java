package com.example.staj1.Dto;

public record CustomerResponse(Integer id, String name, String surname, String email,
                               Integer age, String telNo, boolean active) {}
