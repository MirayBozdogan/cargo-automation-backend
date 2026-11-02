package com.example.staj1.service;

import com.example.staj1.Dto.PriceRequest;
import com.example.staj1.exception.PriceRangeConflictException;
import com.example.staj1.model.Price;
import com.example.staj1.repository.PriceRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PriceService {
    private final PriceRepository priceRepository;
    private final EntityManager entityManager;

    public PriceService(PriceRepository priceRepository, EntityManager entityManager) {
        this.priceRepository = priceRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Price create(PriceRequest request) {
        if (request.getMinDesi() == null || request.getMaxDesi() == null || request.getPrice() == null
                || request.getMinDesi().signum() < 0 || request.getPrice().signum() <= 0
                || request.getMinDesi().compareTo(request.getMaxDesi()) >= 0) {
            throw new IllegalArgumentException("Alt sınır 0 veya daha büyük, üst sınır alt sınırdan büyük ve ücret pozitif olmalıdır.");
        }
        // Tarife sayısı azdır. Kontrol ve ekleme boyunca yazmaları sıraya alır;
        // iki eşzamanlı istek boş tabloda bile çakışan aralık ekleyemez. Okumalar devam eder.
        entityManager.createNativeQuery("LOCK TABLE prices IN SHARE ROW EXCLUSIVE MODE").executeUpdate();
        if (priceRepository.existsByMinDesiLessThanAndMaxDesiGreaterThan(request.getMaxDesi(), request.getMinDesi())) {
            throw new PriceRangeConflictException("Bu fiyat aralığı mevcut bir tarife ile çakışıyor.");
        }
        Price price = new Price();
        price.setMinDesi(request.getMinDesi()); price.setMaxDesi(request.getMaxDesi()); price.setPrice(request.getPrice());
        return priceRepository.save(price);
    }

    public List<Price> getAll() { return priceRepository.findAll(); }
}
