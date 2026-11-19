package com.payflow.service;

import com.payflow.dto.*;
import com.payflow.entity.*;
import com.payflow.exception.ResourceNotFoundException;
import com.payflow.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final SimulatedGateway gateway;
    public PaymentService(PaymentRepository repository, SimulatedGateway gateway) { this.repository = repository; this.gateway = gateway; }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request, String key) {
        Optional<Payment> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) return PaymentResponse.from(existing.get());
        Payment payment = new Payment("txn_" + UUID.randomUUID(), request.amount(), request.currency().toUpperCase(Locale.ROOT),
                request.customerId(), request.paymentMethod(), gateway.process(request), key);
        try { return PaymentResponse.from(repository.saveAndFlush(payment)); }
        catch (DataIntegrityViolationException e) {
            return repository.findByIdempotencyKey(key).map(PaymentResponse::from).orElseThrow(() -> e);
        }
    }
    @Transactional(readOnly = true)
    public PaymentResponse get(String transactionId) {
        return repository.findByTransactionId(transactionId).map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }
    @Transactional(readOnly = true)
    public Page<PaymentResponse> list(PaymentStatus status, PaymentMethod method, Pageable pageable) {
        Page<Payment> page;
        if (status != null && method != null) page = repository.findByStatusAndPaymentMethod(status, method, pageable);
        else if (status != null) page = repository.findByStatus(status, pageable);
        else if (method != null) page = repository.findByPaymentMethod(method, pageable);
        else page = repository.findAll(pageable);
        return page.map(PaymentResponse::from);
    }
    @Transactional(readOnly = true)
    public Map<String, Object> analytics() {
        long total = repository.count();
        long successes = repository.countByStatus(PaymentStatus.SUCCESS);
        long failures = repository.countByStatus(PaymentStatus.FAILED);
        BigDecimal revenue = Optional.ofNullable(repository.sumAmountByStatus(PaymentStatus.SUCCESS)).orElse(BigDecimal.ZERO);
        Map<String, Map<String, Object>> byMethod = new LinkedHashMap<>();
        List<Payment> all = repository.findAll();
        for (PaymentMethod method : PaymentMethod.values()) {
            List<Payment> rows = all.stream().filter(p -> p.getPaymentMethod() == method).toList();
            byMethod.put(method.name(), Map.of("count", rows.size(), "successfulCount", rows.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).count(),
                    "revenue", rows.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return Map.of("totalPayments", total, "successfulPayments", successes, "failedPayments", failures,
                "totalRevenue", revenue, "failureRate", total == 0 ? 0.0 : (double) failures / total, "byPaymentMethod", byMethod);
    }
}




