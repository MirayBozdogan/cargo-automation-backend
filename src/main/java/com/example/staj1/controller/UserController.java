package com.example.staj1.controller;

import com.example.staj1.Dto.UserRequest;
import com.example.staj1.Dto.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.util.HashMap;
import com.example.staj1.Dto.CurrentUserResponse;
import com.example.staj1.model.User;
import com.example.staj1.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<CustomerResponse> customers(@RequestParam Map<String, String> parameters,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Map<String, String> filters = new HashMap<>(parameters);
        filters.keySet().removeAll(java.util.Set.of("page", "size", "sort"));
        return userService.searchCustomers(filters, pageable);
    }

    @GetMapping("/me")
    public CurrentUserResponse getMe() {
        return userService.getMe();
    }

    @GetMapping("/{id}")
    public User userGet(@PathVariable Integer id) {
        return userService.userGet(id);
    }

    @GetMapping("/search")
    public List<User> search(
            @RequestParam Map<String, String> filters) {
        return userService.search(filters);
    }

    @PostMapping("")
    public User ekle(
            @Valid @RequestBody UserRequest userRequest) {

        return userService.ekle(userRequest);
    }

    @PutMapping("/{id}")
    public User guncelle(
            @PathVariable Integer id,
            @Valid @RequestBody UserRequest userRequest) {

        return userService.guncelle(id, userRequest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Integer id) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }
}