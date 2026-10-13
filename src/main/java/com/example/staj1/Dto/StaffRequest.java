package com.example.staj1.Dto;

import com.example.staj1.config.StrictIntegerDeserializer;
import com.example.staj1.config.StrictStringDeserializer;
import jakarta.validation.constraints.*;
import tools.jackson.databind.annotation.JsonDeserialize;

public class StaffRequest {
    @NotBlank(message = "İsim boş olamaz.")
    @Size(max = 100, message = "İsim en fazla 100 karakter olabilir.")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String name;

    @NotBlank(message = "Soyisim boş olamaz.")
    @Size(max = 100, message = "Soyisim en fazla 100 karakter olabilir.")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String surname;

    @NotBlank(message = "E-posta boş olamaz.")
    @Email(message = "Geçerli e-posta giriniz.")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String email;

    @NotNull(message = "Yaş boş olamaz.")
    @Min(value = 18, message = "18 yaşından küçük olamaz.")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer age;

    @NotBlank(message = "TC kimlik numarası boş olamaz.")
    @Pattern(regexp = "[0-9]{11}", message = "TC kimlik numarası 11 rakam olmalıdır.")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String tc;

    @NotBlank(message = "Telefon boş olamaz.")
    @Pattern(regexp = "05[0-9]{9}", message = "Telefon 05 ile başlayan 11 rakam olmalıdır.")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String telNo;

    @NotBlank(message = "Şifre boş olamaz.")
    @Size(min = 6, max = 72, message = "Şifre 6-72 karakter arasında olmalıdır.")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String password;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getTc() { return tc; }
    public void setTc(String tc) { this.tc = tc; }

    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}
