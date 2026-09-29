package com.example.staj1.repository;

import com.example.staj1.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    List<Address> findByUserId(Integer user_id);
    boolean existsByUserId(Integer user_id);
    boolean existsByCityId(Integer cityId);

    boolean existsByDistrictId(Integer id);
    boolean existsByIdAndUserId(Integer id, Integer userId);
}