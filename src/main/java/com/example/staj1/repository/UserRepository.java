package com.example.staj1.repository;

import com.example.staj1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository
        extends JpaRepository<User, Integer>,
        JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    boolean existsByTc(String tc);

    boolean existsByTelNo(String telNo);

    boolean existsByEmailAndIdNot(String email, Integer id);
    boolean existsByTcAndIdNot(String tc, Integer id);
    boolean existsByTelNoAndIdNot(String telNo, Integer id);

    Page<User> findByRole(String role, Pageable pageable);

    Optional<User> findByEmail(String email);
}
