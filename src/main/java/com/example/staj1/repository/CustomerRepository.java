package com.example.staj1.repository;

import com.example.staj1.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Integer>,
        JpaSpecificationExecutor<Customer> {

    boolean existsByEmail(String email);

    boolean existsByTc(String tc);

    boolean existsByTelNo(String telNo);

    Optional<Customer> findByEmail(String email);
}
