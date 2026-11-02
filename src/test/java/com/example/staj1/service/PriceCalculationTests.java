package com.example.staj1.service;

import com.example.staj1.exception.PriceRangeConflictException;
import com.example.staj1.model.Price;
import com.example.staj1.repository.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PriceCalculationTests {
    PriceRepository prices; ShipmentService service;
    @BeforeEach void setup() {
        prices = mock(PriceRepository.class);
        service = new ShipmentService(mock(ShipmentRepository.class), prices, mock(UserRepository.class), mock(AddressRepository.class));
        Price price = new Price(); price.setPrice(new BigDecimal("80"));
        when(prices.findByMinDesiLessThanEqualAndMaxDesiGreaterThan(any(), any())).thenReturn(price);
    }
    @Test void largerOfDesiAndWeightDeterminesPrice() {
        var byDesi = service.calculatePriceDetail(new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("40"), BigDecimal.ONE);
        assertEquals("DESI", byDesi.getCalculatedBy()); assertEquals(new BigDecimal("8.00"), byDesi.getCalculatedValue());
        var byWeight = service.calculatePriceDetail(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("9"));
        assertEquals("WEIGHT", byWeight.getCalculatedBy()); assertEquals(new BigDecimal("9"), byWeight.getCalculatedValue());
    }
    @Test void equalDesiAndWeightKeepsExistingWeightRule() {
        var result = service.calculatePriceDetail(new BigDecimal("30"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE);
        assertEquals("WEIGHT", result.getCalculatedBy()); assertEquals(0, BigDecimal.ONE.compareTo(result.getCalculatedValue()));
    }
    @Test void desiRoundingKeepsTwoDecimalHalfUpRuleAtTariffBoundary() {
        var result = service.calculatePriceDetail(new BigDecimal("29.85"), BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("0.1"));
        assertEquals(new BigDecimal("1.00"), result.getDesi());
        verify(prices).findByMinDesiLessThanEqualAndMaxDesiGreaterThan(new BigDecimal("1.00"), new BigDecimal("1.00"));
    }
    @Test void invalidDimensionsAreRejectedBeforeTariffLookup() {
        reset(prices);
        for (int i = 0; i < 4; i++) {
            for (BigDecimal invalid : new BigDecimal[]{null, BigDecimal.ZERO, new BigDecimal("-1")}) {
                BigDecimal[] values = {BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE}; values[i] = invalid;
                assertThrows(IllegalArgumentException.class, () -> service.calculatePriceDetail(values[0],values[1],values[2],values[3]));
            }
        }
        verifyNoInteractions(prices);
    }
    @Test void legacyOverlapsHaveADomainConflictMessage() {
        when(prices.findByMinDesiLessThanEqualAndMaxDesiGreaterThan(any(), any()))
                .thenThrow(new IncorrectResultSizeDataAccessException(1, 2));
        assertThrows(PriceRangeConflictException.class, () -> service.calculatePriceDetail(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE));
    }
}
