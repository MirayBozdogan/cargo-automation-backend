package com.example.staj1.service;

import com.example.staj1.Dto.UserRequest;
import com.example.staj1.Dto.CurrentUserResponse;
import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.model.User;
import com.example.staj1.repository.AddressRepository;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.specification.UserSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public UserService(
            UserRepository userRepository,
            AddressRepository addressRepository) {

        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    public CurrentUserResponse getMe() {
        User user = getCurrentUser();
        return new CurrentUserResponse(user.getId(), user.getName(), user.getSurname(),
                user.getEmail(), user.getAge(), user.getTc(), user.getTelNo(), user.getRole());
    }

    public User userGet(Integer id) {

        User currentUser = getCurrentUser();

        if (!currentUser.getId().equals(id)) {
            throw new AccessDeniedException(
                    "Bu kullanıcıya erişim yetkiniz yok."
            );
        }

        return currentUser;
    }

    public List<User> search(Map<String, String> filters) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        filters.put("email", email);

        Specification<User> specification =
                UserSpecification.filter(filters);

        return userRepository.findAll(specification);
    }

    public User ekle(UserRequest userRequest) {

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new IllegalArgumentException(
                    "Bu e-posta adresi zaten kayıtlı!"
            );
        }

        if (userRepository.existsByTc(userRequest.getTc())) {
            throw new IllegalArgumentException(
                    "Bu TC zaten kayıtlı!"
            );
        }

        if (userRepository.existsByTelNo(userRequest.getTelNo())) {
            throw new IllegalArgumentException(
                    "Bu numara zaten kayıtlı!"
            );
        }

        User user = new User();

        user.setName(userRequest.getName());
        user.setSurname(userRequest.getSurname());
        user.setAge(userRequest.getAge());
        user.setEmail(userRequest.getEmail());
        user.setTc(userRequest.getTc());
        user.setTelNo(userRequest.getTelNo());

        return userRepository.save(user);
    }

    public User guncelle(Integer id, UserRequest userRequest) {

        User user = userGet(id);

        if (userRepository.existsByEmail(userRequest.getEmail())
                && !user.getEmail().equals(userRequest.getEmail())) {

            throw new GlobalExceptionHandler.DuplicateResourceException(
                    "Bu e-posta adresi başka bir kullanıcıda kayıtlı!"
            );
        }

        if (userRepository.existsByTc(userRequest.getTc())
                && !user.getTc().equals(userRequest.getTc())) {

            throw new GlobalExceptionHandler.DuplicateResourceException(
                    "Bu TC başka bir kullanıcıda kayıtlı!"
            );
        }

        if (userRepository.existsByTelNo(userRequest.getTelNo())
                && !user.getTelNo().equals(userRequest.getTelNo())) {

            throw new GlobalExceptionHandler.DuplicateResourceException(
                    "Bu numara başka bir kullanıcıda kayıtlı!"
            );
        }

        user.setName(userRequest.getName());
        user.setSurname(userRequest.getSurname());
        user.setEmail(userRequest.getEmail());
        user.setAge(userRequest.getAge());
        user.setTc(userRequest.getTc());
        user.setTelNo(userRequest.getTelNo());

        return userRepository.save(user);
    }

    public void deleteUser(Integer id) {

        User user = userGet(id);

        if (addressRepository.existsByUserId(id)) {
            throw new GlobalExceptionHandler.DuplicateResourceException(
                    "Bu kullanıcıya ait adres kayıtları olduğu için silinemez."
            );
        }

        userRepository.delete(user);
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Kullanıcı bulunamadı."
                        ));
    }
}