package com.example.staj1.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.staj1.Dto.AddressRequest;
import com.example.staj1.model.Address;
import com.example.staj1.model.City;
import com.example.staj1.model.Customer;
import com.example.staj1.model.District;
import com.example.staj1.repository.AddressRepository;
import com.example.staj1.repository.CityRepository;
import com.example.staj1.repository.CustomerRepository;
import com.example.staj1.repository.DistrictRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;

    public AddressService(AddressRepository addressRepository,
                          CustomerRepository customerRepository,
                          CityRepository cityRepository,
                          DistrictRepository districtRepository) {

        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
    }


    public List<Address> getById(Integer customerId) {

        Customer currentCustomer = getCurrentCustomer();

        if (!currentCustomer.getId().equals(customerId)) {
            throw new AccessDeniedException("Bu müşterinin adreslerine erişemezsiniz.");
        }

        List<Address> addresses = addressRepository.findByCustomerId(customerId);

        if (addresses.isEmpty()) {
            throw new EntityNotFoundException(
                    "Bu müşteriye ait kayıtlı adres bulunamadı."
            );
        }

        return addresses;
    }

    public Address create(AddressRequest addressRequest, Integer customerId) {

        Customer currentCustomer = getCurrentCustomer();

        if (!currentCustomer.getId().equals(customerId)) {
            throw new AccessDeniedException(
                    "Bu müşteri adına adres ekleyemezsiniz."
            );
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Müşteri bulunamadı.")
                );

        City city = cityRepository.findById(addressRequest.getCityId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Şehir bulunamadı."
                ));

        District district = districtRepository.findById(addressRequest.getDistrictId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "İlçe bulunamadı."
                ));

        if (!district.getCity().getId().equals(city.getId())) {
            throw new IllegalArgumentException(
                    "Seçilen ilçe, seçilen şehre ait değil."
            );
        }

        Address address = new Address();

        address.setCity(city);
        address.setDistrict(district);
        address.setNeighborhood(addressRequest.getNeighborhood());
        address.setBuildingNo(addressRequest.getBuildingNo());
        address.setApartmentNo(addressRequest.getApartmentNo());
        address.setCustomer(customer);
        return addressRepository.save(address);
    }

    public Address update(Integer customer_id, Integer id, AddressRequest addressRequest) {
        Customer currentCustomer = getCurrentCustomer();

        if (!currentCustomer.getId().equals(customer_id)) {
            throw new AccessDeniedException(
                    "Bu müşterinin adresini güncelleyemezsiniz."
            );
        }

        Customer customer = customerRepository.findById(customer_id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Müşteri bulunamadı."));

        Address address = addressRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Adres bulunamadı."));

        if (!address.getCustomer().getId().equals(customer_id)) {
            throw new IllegalArgumentException(
                    "Bu adres bu müşteriye ait değil."
            );
        }

        City city = cityRepository.findById(addressRequest.getCityId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Şehir bulunamadı."));

        District district = districtRepository.findById(addressRequest.getDistrictId())
                .orElseThrow(() ->
                        new EntityNotFoundException("İlçe bulunamadı."));

        if (!district.getCity().getId().equals(city.getId())) {
            throw new IllegalArgumentException(
                    "Seçilen ilçe, seçilen şehre ait değil."
            );
        }

        address.setCity(city);
        address.setDistrict(district);
        address.setNeighborhood(addressRequest.getNeighborhood());
        address.setBuildingNo(addressRequest.getBuildingNo());
        address.setApartmentNo(addressRequest.getApartmentNo());
        address.setCustomer(customer);

        return addressRepository.save(address);
    }

    public void delete(Integer customer_id, Integer id) {

        Customer currentCustomer = getCurrentCustomer();

        if (!currentCustomer.getId().equals(customer_id)) {
            throw new AccessDeniedException(
                    "Bu müşterinin adresini silemezsiniz."
            );
        }

        Customer customer = customerRepository.findById(customer_id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Müşteri bulunamadı."));

        Address address = addressRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Adres bulunamadı."));

        if (!address.getCustomer().getId().equals(customer_id)) {
            throw new IllegalArgumentException(
                    "Bu adres bu müşteriye ait değil."
            );
        }

        addressRepository.delete(address);
    }

    private Customer getCurrentCustomer() {
        return (Customer) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}