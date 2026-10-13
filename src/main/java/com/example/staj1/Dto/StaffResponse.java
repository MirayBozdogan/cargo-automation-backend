package com.example.staj1.Dto;

public record StaffResponse(Integer id, String name, String surname, String email,
                            Integer age, String tc, String telNo, String role, boolean active) {}
