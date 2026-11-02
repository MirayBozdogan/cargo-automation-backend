package com.example.staj1.repository;

import com.example.staj1.Dto.PriceRequest;
import com.example.staj1.exception.PriceRangeConflictException;
import com.example.staj1.model.Price;
import com.example.staj1.service.PriceService;
import com.example.staj1.service.ShipmentService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PriceBoundaryPersistenceTests {
    @Autowired PriceRepository prices;
    @Autowired PriceService priceService;
    @Autowired ShipmentService shipments;
    @BeforeEach void setup() { prices.deleteAll(); prices.flush(); }
    PriceRequest request(String min, String max, String amount) {
        PriceRequest request = new PriceRequest(); request.setMinDesi(new BigDecimal(min));
        request.setMaxDesi(new BigDecimal(max)); request.setPrice(new BigDecimal(amount)); return request;
    }
    BigDecimal quote(String weight) {
        return shipments.calculatePrice(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, new BigDecimal(weight));
    }
    @Test void touchingRangesAreAllowedAndBoundaryUsesNextTariff() {
        priceService.create(request("0", "1", "60")); priceService.create(request("1", "3", "80"));
        priceService.create(request("3", "5", "100")); prices.flush();
        assertEquals(0, new BigDecimal("60").compareTo(quote("0.99")));
        assertEquals(0, new BigDecimal("80").compareTo(quote("1")));
        assertEquals(0, new BigDecimal("80").compareTo(quote("2.99")));
        assertEquals(0, new BigDecimal("100").compareTo(quote("3")));
        assertThrows(EntityNotFoundException.class, () -> quote("5"));
    }
    @Test void overlapQueryRejectsContainedContainingPartialAndDuplicateRanges() {
        priceService.create(request("1", "3", "80")); prices.flush();
        for (String[] bounds : new String[][]{{"1","3"},{"1.5","2"},{"0","4"},{"0","2"},{"2","4"}}) {
            assertTrue(prices.existsByMinDesiLessThanAndMaxDesiGreaterThan(
                    new BigDecimal(bounds[1]), new BigDecimal(bounds[0])));
        }
        assertFalse(prices.existsByMinDesiLessThanAndMaxDesiGreaterThan(new BigDecimal("1"), BigDecimal.ZERO));
        assertFalse(prices.existsByMinDesiLessThanAndMaxDesiGreaterThan(new BigDecimal("4"), new BigDecimal("3")));
    }
    @Test void overlappingCreationDoesNotInsertAnotherTariff() {
        priceService.create(request("1", "3", "80")); prices.flush();
        assertThrows(PriceRangeConflictException.class, () -> priceService.create(request("2", "4", "100")));
        assertEquals(1, prices.count());
    }
    @Test void gapNeverFallsBackToAnotherTariff() {
        priceService.create(request("0", "1", "60")); priceService.create(request("2", "3", "80"));
        assertThrows(EntityNotFoundException.class, () -> quote("1.5"));
    }
    @Test void existingOverlappingDataReturnsConflictInsteadOfChoosingArbitraryPrice() {
        for (String[] bounds : new String[][]{{"0","3"},{"1","4"}}) {
            Price price = new Price(); price.setMinDesi(new BigDecimal(bounds[0]));
            price.setMaxDesi(new BigDecimal(bounds[1])); price.setPrice(BigDecimal.TEN); prices.saveAndFlush(price);
        }
        assertThrows(PriceRangeConflictException.class, () -> quote("2"));
    }
}
