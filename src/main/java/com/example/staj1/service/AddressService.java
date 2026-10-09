package com.example.staj1.service;

import com.example.staj1.model.*;
import com.example.staj1.Dto.AddressRequest;
import com.example.staj1.repository.AddressRepository;
import com.example.staj1.repository.CityRepository;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.repository.DistrictRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;

    public AddressService(
            AddressRepository addressRepository,
            UserRepository userRepository,
            CityRepository cityRepository,
            DistrictRepository districtRepository) {

        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
    }

    public List<Address> getById(Integer userId) {

        User currentUser = getCurrentUser();

        if (!currentUser.getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Bu kullanıcının adreslerine erişemezsiniz."
            );
        }

        return addressRepository.findByUserId(userId);
    }

    public Address create(
            AddressRequest addressRequest,
            Integer userId) {

        User currentUser = getCurrentUser();

        if (!currentUser.getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Bu kullanıcı adına adres ekleyemezsiniz."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Kullanıcı bulunamadı."
                        )
                );

        City city = cityRepository.findById(addressRequest.getCityId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Şehir bulunamadı."
                        )
                );

        District district =
                districtRepository.findById(
                        addressRequest.getDistrictId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "İlçe bulunamadı."
                        )
                );

        if (!district.getCity().getId().equals(city.getId())) {
            throw new IllegalArgumentException(
                    "Seçilen ilçe, seçilen şehre ait değil."
            );
        }

        Address address = new Address();

        address.setCity(city);
        address.setDistrict(district);
        address.setNeighborhood(
                addressRequest.getNeighborhood()
        );
        address.setBuildingNo(
                addressRequest.getBuildingNo()
        );
        address.setApartmentNo(
                addressRequest.getApartmentNo()
        );
        address.setUser(user);

        return addressRepository.save(address);
    }

    public Address update(
            Integer userId,
            Integer id,
            AddressRequest addressRequest) {

        User currentUser = getCurrentUser();

        if (!currentUser.getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Bu kullanıcının adresini güncelleyemezsiniz."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Kullanıcı bulunamadı."
                        )
                );

        Address address = addressRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Adres bulunamadı."
                        )
                );

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Bu adres bu kullanıcıya ait değil."
            );
        }

        City city = cityRepository.findById(
                addressRequest.getCityId()
        ).orElseThrow(() ->
                new EntityNotFoundException(
                        "Şehir bulunamadı."
                )
        );

        District district = districtRepository.findById(
                addressRequest.getDistrictId()
        ).orElseThrow(() ->
                new EntityNotFoundException(
                        "İlçe bulunamadı."
                )
        );

        if (!district.getCity().getId().equals(city.getId())) {
            throw new IllegalArgumentException(
                    "Seçilen ilçe, seçilen şehre ait değil."
            );
        }

        address.setCity(city);
        address.setDistrict(district);
        address.setNeighborhood(
                addressRequest.getNeighborhood()
        );
        address.setBuildingNo(
                addressRequest.getBuildingNo()
        );
        address.setApartmentNo(
                addressRequest.getApartmentNo()
        );
        address.setUser(user);

        return addressRepository.save(address);
    }

    public void delete(Integer userId, Integer id) {

        User currentUser = getCurrentUser();

        if (!currentUser.getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Bu kullanıcının adresini silemezsiniz."
            );
        }

        Address address = addressRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Adres bulunamadı."
                        )
                );

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Bu adres bu kullanıcıya ait değil."
            );
        }

        addressRepository.delete(address);
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}