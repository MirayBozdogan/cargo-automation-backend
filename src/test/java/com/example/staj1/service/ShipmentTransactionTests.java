package com.example.staj1.service;

import com.example.staj1.exception.ShipmentStateException;
import com.example.staj1.model.Shipment;
import com.example.staj1.model.ShipmentStatus;
import com.example.staj1.model.User;
import com.example.staj1.repository.ShipmentRepository;
import com.example.staj1.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ShipmentTransactionTests {
    @Autowired ShipmentService service;
    @Autowired ShipmentRepository shipments;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    TransactionTemplate transaction;
    ExecutorService workers;
    Integer shipmentId;
    Integer customerId;
    Integer staffId;
    String customerEmail;
    String staffEmail;

    @BeforeEach void setup() {
        transaction = new TransactionTemplate(transactionManager);
        workers = Executors.newFixedThreadPool(2);
        String suffix = UUID.randomUUID().toString();
        customerEmail = "customer-" + suffix + "@example.test";
        staffEmail = "staff-" + suffix + "@example.test";
        transaction.executeWithoutResult(status -> {
            User customer = new User(); customer.setEmail(customerEmail);
            customerId = users.save(customer).getId();
            User staff = new User(); staff.setEmail(staffEmail); staff.setRole("STAFF");
            staffId = users.save(staff).getId();
            Shipment shipment = new Shipment(); shipment.setSender(customer);
            shipment.setBarcode(suffix.replace("-", "").substring(0, 12));
            shipment.setPrice(BigDecimal.TEN);
            shipmentId = shipments.save(shipment).getId();
        });
    }

    @AfterEach void cleanup() throws InterruptedException {
        SecurityContextHolder.clearContext();
        workers.shutdownNow();
        // Finish workers before deleting fixtures, including on assertion failure.
        assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS), "Worker transactions did not finish.");
        transaction.executeWithoutResult(status -> {
            if (shipmentId != null) shipments.deleteById(shipmentId);
            if (customerId != null) users.deleteById(customerId);
            if (staffId != null) users.deleteById(staffId);
        });
    }

    void asUser(String email, Runnable action) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    record Snapshot(ShipmentStatus status, boolean deleted, String barcode,
                    Integer senderId, BigDecimal price, Instant createdAt, Instant deliveredAt) {}

    Snapshot snapshot() {
        return transaction.execute(status -> {
            Shipment shipment = shipments.findById(shipmentId).orElseThrow();
            return new Snapshot(shipment.getStatus(), shipment.getDeleted(), shipment.getBarcode(),
                    shipment.getSender().getId(), shipment.getPrice(), shipment.getCreatedAt(), shipment.getDeliveredAt());
        });
    }

    @Test void acceptanceRollsBackWhenLaterWorkFailsAndCanBeRetried() {
        Snapshot before = snapshot();
        assertThrows(IllegalStateException.class, () -> asUser(staffEmail, () ->
                transaction.executeWithoutResult(status -> {
                    service.accept(shipmentId);
                    shipments.flush(); // Send the change to PostgreSQL before forcing rollback.
                    throw new IllegalStateException("Simulated failure after acceptance");
                })));
        assertEquals(before, snapshot());
        asUser(staffEmail, () -> service.accept(shipmentId));
        assertEquals(ShipmentStatus.ACCEPTED, snapshot().status());
    }

    @Test void preparationRollsBackWithoutLosingPreviousAcceptance() {
        asUser(staffEmail, () -> service.accept(shipmentId));
        Snapshot before = snapshot();
        assertThrows(IllegalStateException.class, () -> asUser(staffEmail, () ->
                transaction.executeWithoutResult(status -> {
                    service.prepare(shipmentId);
                    shipments.flush();
                    throw new IllegalStateException("Simulated failure after preparation");
                })));
        assertEquals(before, snapshot());
        asUser(staffEmail, () -> service.prepare(shipmentId));
        assertEquals(ShipmentStatus.READY_FOR_DISTRIBUTION, snapshot().status());
    }

    @Test void customerCannotChangeOperationalStatusEvenAsSender() {
        Snapshot before = snapshot();
        assertThrows(AccessDeniedException.class, () -> asUser(customerEmail, () -> service.accept(shipmentId)));
        assertEquals(before, snapshot());
        asUser(staffEmail, () -> service.accept(shipmentId));
        Snapshot accepted = snapshot();
        assertThrows(AccessDeniedException.class, () -> asUser(customerEmail, () -> service.prepare(shipmentId)));
        assertEquals(accepted, snapshot());
    }

    @Test void staffCannotDeleteAnotherCustomersCreatedShipment() {
        Snapshot before = snapshot();
        assertThrows(AccessDeniedException.class, () -> asUser(staffEmail, () -> service.delete(shipmentId)));
        assertEquals(before, snapshot());
    }

    @Test void simultaneousAcceptancesCommitOnlyOneTransition() throws Exception {
        assertContends(staffEmail, () -> service.accept(shipmentId),
                staffEmail, () -> service.accept(shipmentId), ShipmentStateException.class);
        assertEquals(ShipmentStatus.ACCEPTED, snapshot().status());
        assertFalse(snapshot().deleted());
    }

    @Test void deletionWaitsForAcceptanceAndThenRechecksStatus() throws Exception {
        assertContends(staffEmail, () -> service.accept(shipmentId),
                customerEmail, () -> service.delete(shipmentId), ShipmentStateException.class);
        assertEquals(ShipmentStatus.ACCEPTED, snapshot().status());
        assertFalse(snapshot().deleted());
    }

    @Test void acceptanceWaitsForDeletionAndThenRejectsDeletedShipment() throws Exception {
        assertContends(customerEmail, () -> service.delete(shipmentId),
                staffEmail, () -> service.accept(shipmentId), EntityNotFoundException.class);
        assertEquals(ShipmentStatus.CREATED, snapshot().status());
        assertTrue(snapshot().deleted());
    }

    @Test void simultaneousPreparationsCommitOnlyOneTransition() throws Exception {
        asUser(staffEmail, () -> service.accept(shipmentId));
        assertContends(staffEmail, () -> service.prepare(shipmentId),
                staffEmail, () -> service.prepare(shipmentId), ShipmentStateException.class);
        assertEquals(ShipmentStatus.READY_FOR_DISTRIBUTION, snapshot().status());
        assertNull(snapshot().deliveredAt());
    }

    /** Keep the first transaction open until PostgreSQL confirms the second is waiting on its lock. */
    void assertContends(String firstUser, Runnable firstAction, String secondUser, Runnable secondAction,
                        Class<? extends Throwable> expectedFailure) throws Exception {
        CountDownLatch firstHoldsLock = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        CountDownLatch secondConnected = new CountDownLatch(1);
        AtomicInteger secondPid = new AtomicInteger();
        Future<?> first = workers.submit(() -> asUser(firstUser, () ->
                transaction.executeWithoutResult(status -> {
                    jdbc.execute("SET LOCAL lock_timeout = '10s'");
                    firstAction.run();
                    firstHoldsLock.countDown();
                    await(allowFirstCommit);
                })));
        Future<?> second = null;
        try {
            assertTrue(firstHoldsLock.await(10, TimeUnit.SECONDS), "First transaction never acquired its lock.");
            second = workers.submit(() -> asUser(secondUser, () ->
                    transaction.executeWithoutResult(status -> {
                        jdbc.execute("SET LOCAL lock_timeout = '10s'");
                        secondPid.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                        secondConnected.countDown();
                        secondAction.run();
                    })));
            assertTrue(secondConnected.await(10, TimeUnit.SECONDS), "Second transaction did not connect.");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            boolean blocked = false;
            while (System.nanoTime() < deadline && !second.isDone()) {
                blocked = Boolean.TRUE.equals(jdbc.queryForObject(
                        "select cardinality(pg_blocking_pids(?)) > 0", Boolean.class, secondPid.get()));
                if (blocked) break;
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
            }
            assertTrue(blocked, "PostgreSQL did not observe the second transaction waiting for a lock.");
            allowFirstCommit.countDown();
            first.get(10, TimeUnit.SECONDS);
            Future<?> contender = second;
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> contender.get(10, TimeUnit.SECONDS));
            assertInstanceOf(expectedFailure, failure.getCause());
        } finally {
            allowFirstCommit.countDown();
            if (!first.isDone()) first.cancel(true);
            if (second != null && !second.isDone()) second.cancel(true);
        }
    }

    static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Transaction coordination timed out.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Transaction worker interrupted.", ex);
        }
    }
}
