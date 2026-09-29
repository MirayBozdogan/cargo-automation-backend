package com.example.staj1.repository;

import com.example.staj1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Integer>,
        JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    boolean existsByTc(String tc);

    boolean existsByTelNo(String telNo);

    Optional<User> findByEmail(String email);
}
