package com.payflow.service;

import com.payflow.dto.CreatePaymentRequest;
import com.payflow.entity.*;
import com.payflow.exception.ResourceNotFoundException;
import com.payflow.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository repository;
    @Mock SimulatedGateway gateway;
    @InjectMocks PaymentService service;

    private CreatePaymentRequest request(String amount) { return new CreatePaymentRequest(new BigDecimal(amount), "inr", "cust-1", PaymentMethod.UPI); }

    @Test void createsPaymentAndUppercasesCurrency() {
        when(repository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(gateway.process(any())).thenReturn(PaymentStatus.SUCCESS);
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.create(request("12.50"), "key-1");
        assertEquals("INR", result.currency()); assertEquals(PaymentStatus.SUCCESS, result.status());
        assertTrue(result.transactionId().startsWith("txn_"));
    }

    @Test void returnsPreviousPaymentForRepeatedIdempotencyKey() {
        Payment prior = new Payment("txn_old", new BigDecimal("12.50"), "INR", "cust-1", PaymentMethod.UPI, PaymentStatus.SUCCESS, "key-1");
        when(repository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(prior));
        var result = service.create(request("12.50"), "key-1");
        assertEquals("txn_old", result.transactionId()); verify(repository, never()).saveAndFlush(any());
    }

    @Test void retrievesExistingAndRejectsMissingTransaction() {
        Payment prior = new Payment("txn_old", new BigDecimal("1.00"), "INR", "cust", PaymentMethod.CARD, PaymentStatus.SUCCESS, "key");
        when(repository.findByTransactionId("txn_old")).thenReturn(Optional.of(prior));
        assertEquals("txn_old", service.get("txn_old").transactionId());
        when(repository.findByTransactionId("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get("missing"));
    }

    @Test void reportsAnalytics() {
        when(repository.count()).thenReturn(3L);
        when(repository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(2L);
        when(repository.countByStatus(PaymentStatus.FAILED)).thenReturn(1L);
        when(repository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("20.00"));
        when(repository.findAll()).thenReturn(List.of(
                new Payment("txn1", new BigDecimal("10"), "INR", "c", PaymentMethod.UPI, PaymentStatus.SUCCESS, "k1"),
                new Payment("txn2", new BigDecimal("10"), "INR", "c", PaymentMethod.CARD, PaymentStatus.SUCCESS, "k2"),
                new Payment("txn3", new BigDecimal("4"), "INR", "c", PaymentMethod.UPI, PaymentStatus.FAILED, "k3")));
        var result = service.analytics();
        assertEquals(2L, result.get("successfulPayments")); assertEquals(0.3333333333333333, result.get("failureRate"));
        assertEquals(new BigDecimal("20.00"), result.get("totalRevenue"));
    }
}
