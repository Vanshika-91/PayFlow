package com.payflow.service;

import com.payflow.dto.CreatePaymentRequest;
import com.payflow.entity.PaymentStatus;
import org.springframework.stereotype.Component;

/** Deterministic simulator: amounts ending in .99 fail; all other requests succeed. */
@Component
public class SimulatedGateway {
    public PaymentStatus process(CreatePaymentRequest request) {
        return request.amount().remainder(new java.math.BigDecimal("1.00"))
                .compareTo(new java.math.BigDecimal("0.99")) == 0 ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
    }
}
