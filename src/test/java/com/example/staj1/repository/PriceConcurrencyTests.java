package com.example.staj1.repository;

import com.example.staj1.Dto.PriceRequest;
import com.example.staj1.exception.PriceRangeConflictException;
import com.example.staj1.service.PriceService;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PriceConcurrencyTests {
    @Autowired PriceService service;
    @Autowired PriceRepository prices;
    @Test void concurrentOverlappingCreatesCommitOnlyOneTariff() throws Exception {
        var inserted = new CopyOnWriteArrayList<Integer>();
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> create = () -> {
            PriceRequest request = new PriceRequest(); request.setMinDesi(new BigDecimal("9000000"));
            request.setMaxDesi(new BigDecimal("9000001")); request.setPrice(BigDecimal.TEN);
            ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS));
            try { inserted.add(service.create(request).getId()); return true; }
            catch (PriceRangeConflictException expected) { return false; }
        };
        try {
            Future<Boolean> first = executor.submit(create), second = executor.submit(create);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, inserted.size());
            assertTrue(prices.findById(inserted.getFirst()).isPresent());
        } finally {
            start.countDown(); executor.shutdownNow(); executor.awaitTermination(15, TimeUnit.SECONDS);
            prices.deleteAllById(inserted);
        }
    }
}
