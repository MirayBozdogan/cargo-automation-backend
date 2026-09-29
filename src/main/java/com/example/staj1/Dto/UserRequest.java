package com.example.staj1.Dto;

import com.example.staj1.config.StrictIntegerDeserializer;
import com.example.staj1.config.StrictStringDeserializer;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import tools.jackson.databind.annotation.JsonDeserialize;

public class UserRequest {

    @NotBlank(message = "İsim boş olamaz.")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String name;

    @NotBlank(message = "Soyisim boş olamaz.")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String surname;

    @NotBlank(message = "Email boş olamaz.")
    @Email(message = "Geçerli email giriniz.")
    @Column(unique = true)
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String email;

    @NotNull(message = "Yaş boş olamaz.")
    @Min(value = 18, message = "18 yaşından küçük olamaz.")
    @Positive(message = "Yaş 0'dan büyük olmalıdır.")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer age;

    @NotBlank(message = "TC kimlik numarası boş olamaz.")
    @Size(min = 11, max = 11, message = "TC kimlik numarası  11 haneli olmalıdır.")
    @Column(unique = true)
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String tc;

    @NotBlank(message = "Telefon numarası boş olamaz.")
    @Size(min = 11, max = 11, message = "Telefon numarası  11 haneli olmalıdır.")
    @Pattern(regexp = "^0.*", message = "Telefon numarası 0 ile başlamalıdır.")
    @Pattern(regexp = "^05.*", message = "Geçerli bir telefon numarası giriniz.")
    @Column(unique = true)
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String telNo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getTc() {
        return tc;
    }

    public void setTc(String tc) {
        this.tc = tc;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }
}
