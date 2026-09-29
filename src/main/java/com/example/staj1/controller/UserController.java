package com.example.staj1.controller;

import com.example.staj1.Dto.UserRequest;
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