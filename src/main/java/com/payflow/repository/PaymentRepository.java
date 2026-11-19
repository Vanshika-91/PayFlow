package com.payflow.repository;

import com.payflow.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Page<Payment> findByStatusAndPaymentMethod(PaymentStatus status, PaymentMethod paymentMethod, Pageable pageable);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    Page<Payment> findByPaymentMethod(PaymentMethod paymentMethod, Pageable pageable);
    List<Payment> findByStatus(PaymentStatus status);
    long countByStatus(PaymentStatus status);
    BigDecimal sumAmountByStatus(PaymentStatus status);
}
