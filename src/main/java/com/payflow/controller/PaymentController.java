package com.payflow.controller;

import com.payflow.dto.*;
import com.payflow.entity.*;
import com.payflow.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String key) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, key));
    }
    @GetMapping("/payments/{transactionId}")
    public PaymentResponse get(@PathVariable String transactionId) { return service.get(transactionId); }
    @GetMapping("/payments")
    public Page<PaymentResponse> list(@RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, paymentMethod, pageable);
    }
    @GetMapping("/analytics")
    public Map<String, Object> analytics() { return service.analytics(); }
}
