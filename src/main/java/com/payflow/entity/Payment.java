package com.payflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_status_created", columnList = "status, created_at"),
    @Index(name = "idx_payment_method", columnList = "payment_method"),
    @Index(name = "idx_payment_customer", columnList = "customer_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_payment_idempotency_key", columnNames = "idempotency_key"))
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "transaction_id", nullable = false, unique = true, length = 40)
    private String transactionId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentStatus status;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {}
    public Payment(String transactionId, BigDecimal amount, String currency, String customerId, PaymentMethod paymentMethod, PaymentStatus status, String idempotencyKey) {
        this.transactionId = transactionId; this.amount = amount; this.currency = currency;
        this.customerId = customerId; this.paymentMethod = paymentMethod; this.status = status;
        this.idempotencyKey = idempotencyKey; this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCustomerId() { return customerId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
