package com.payflow.dto;

import com.payflow.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreatePaymentRequest(
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
    @NotBlank @Size(max = 100) String customerId,
    @NotNull PaymentMethod paymentMethod
) {}
