package com.payflow.dto;

import com.payflow.entity.Payment;
import com.payflow.entity.PaymentMethod;
import com.payflow.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(String transactionId, BigDecimal amount, String currency, String customerId,
        PaymentMethod paymentMethod, PaymentStatus status, Instant createdAt, Instant updatedAt) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getTransactionId(), p.getAmount(), p.getCurrency(), p.getCustomerId(),
                p.getPaymentMethod(), p.getStatus(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
