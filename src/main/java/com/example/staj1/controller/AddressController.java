package com.example.staj1.controller;

import com.example.staj1.Dto.AddressRequest;
import com.example.staj1.model.Address;
import com.example.staj1.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class AddressController {
    public final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/{user_id}/address")
    public List<Address> getById(@PathVariable Integer user_id) {
        return addressService.getById(user_id);
    }

    @PostMapping("/{user_id}/address")
    public Address create(
            @PathVariable Integer user_id,
            @Valid @RequestBody AddressRequest addressRequest) {
        return addressService.create(addressRequest, user_id);
    }

    @PutMapping("/{user_id}/address/{id}")
    public Address update(@PathVariable Integer user_id,
                          @PathVariable Integer id,
                          @Valid @RequestBody AddressRequest addressRequest) {
        return addressService.update(user_id, id, addressRequest);
    }

    @DeleteMapping("/{user_id}/address/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer user_id,
            @PathVariable Integer id) {

        addressService.delete(user_id, id);

        return ResponseEntity.noContent().build();
    }


}
