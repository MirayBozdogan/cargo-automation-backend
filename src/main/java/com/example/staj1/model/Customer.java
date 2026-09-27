package com.example.staj1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String surname;

    @Column(unique = true)
    private String email;

    private Integer age;

    @Column(unique = true)
    private String tc;

    @Column(unique = true)
    private String telNo;

    @JsonIgnore
    private String password;

    private String role;

    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    private List<Address> addresses;


    public Customer() {
    }


    public Customer(String name, String surname, String email, Integer age,
                    String tc, String telNo, String password, String role) {

        this.name = name;
        this.surname = surname;
        this.email = email;
        this.age = age;
        this.tc = tc;
        this.telNo = telNo;
        this.password = password;
        this.role = role;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


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


    public void setPassword(String password) {
        this.password = password;
    }


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    // Spring Security'nin kullanıcı adını aldığı metot.
    // Biz kullanıcı adı olarak email kullanıyoruz.
    @Override
    public String getUsername() {
        return email;
    }


    // Spring Security'nin şifreyi aldığı metot.
    @Override
    public String getPassword() {
        return password;
    }


    // Kullanıcının yetkilerini belirler.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
    }


    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                ", tc='" + tc + '\'' +
                ", telNo='" + telNo + '\'' +
                '}';
    }
}