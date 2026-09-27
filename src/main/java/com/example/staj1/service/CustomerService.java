package com.example.staj1.service;
import com.example.staj1.Dto.CustomerRequest;
import com.example.staj1.exception.GlobalExceptionHandler;
import com.example.staj1.model.Customer;
import com.example.staj1.repository.AddressRepository;
import com.example.staj1.repository.CustomerRepository;
import com.example.staj1.specification.CustomerSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;

    public CustomerService(CustomerRepository customerRepository, AddressRepository addressRepository) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
    }


    public Customer customerGet(Integer id) {

        Customer currentCustomer = getCurrentCustomer();

        if (!currentCustomer.getId().equals(id)) {
            throw new AccessDeniedException(
                    "Bu müşteriye erişim yetkiniz yok."
            );
        }

        return currentCustomer;
    }


    public List<Customer> search(Map<String, String> filters) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        filters.put("email", email);

        Specification<Customer> specification =
                CustomerSpecification.filter(filters);

        return customerRepository.findAll(specification);
    }

    public Customer ekle(CustomerRequest customerRequest){


        if(customerRepository.existsByEmail(customerRequest.getEmail()))
        {
            throw new IllegalArgumentException("Bu e-posta adresi zaten kayıtlı!");
        }

        if(customerRepository.existsByTc(customerRequest.getTc()))
        {
            throw new IllegalArgumentException("Bu TC zaten kayıtlı!");
        }

        if(customerRepository.existsByTelNo(customerRequest.getTelNo()))
        {
            throw new IllegalArgumentException("Bu numara zaten kayıtlı!");
        }

        Customer customer = new Customer();
       customer.setName(customerRequest.getName());
       customer.setSurname(customerRequest.getSurname());
       customer.setAge(customerRequest.getAge());
       customer.setEmail(customerRequest.getEmail());
       customer.setTc(customerRequest.getTc());
       customer.setTelNo(customerRequest.getTelNo());

      return customerRepository.save(customer);
    }

    public Customer guncelle(Integer id, CustomerRequest customerRequest) {

        Customer customer = customerGet(id);


        if (customerRepository.existsByEmail(customerRequest.getEmail())
                && !customer.getEmail().equals(customerRequest.getEmail())) {
            throw new GlobalExceptionHandler.DuplicateResourceException(
                    "Bu e-posta adresi başka bir müşteride kayıtlı!"
            );
        }

        if (customerRepository.existsByTc(customerRequest.getTc())
                && !customer.getTc().equals(customerRequest.getTc())) {
            throw new GlobalExceptionHandler.DuplicateResourceException(
                    "Bu TC başka bir müşteride kayıtlı!"
            );
        }

        if (customerRepository.existsByTelNo(customerRequest.getTelNo())
                && !customer.getTelNo().equals(customerRequest.getTelNo())) {
            throw new GlobalExceptionHandler.DuplicateResourceException(
                    "Bu telefon numarası başka bir müşteride kayıtlı!"
            );
        }

        customer.setName(customerRequest.getName());
        customer.setSurname(customerRequest.getSurname());
        customer.setEmail(customerRequest.getEmail());
        customer.setAge(customerRequest.getAge());
        customer.setTc(customerRequest.getTc());
        customer.setTelNo(customerRequest.getTelNo());

        return customerRepository.save(customer);
    }

    public void deleteCustomer(Integer id) {

        Customer customer = customerGet(id);

        if (addressRepository.existsByCustomerId(id)) {
            throw new GlobalExceptionHandler.DuplicateResourceException(
                    "Bu müşteriye ait adres kayıtları olduğu için silinemez."
            );
        }

        customerRepository.delete(customer);
    }

    private Customer getCurrentCustomer() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return customerRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("Müşteri bulunamadı."));
    }


}