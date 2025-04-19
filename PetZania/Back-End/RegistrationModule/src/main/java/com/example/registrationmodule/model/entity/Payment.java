package com.example.registrationmodule.model.entity;

import com.example.registrationmodule.model.enumeration.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;  // Auto-generated primary key

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;  // PayPal order ID (unique for each payment)

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;  // Enum for status (COMPLETED, FAILED, PENDING)

    @Column(name = "payer_name", nullable = false, length = 255)
    private String payerName;  // Full name of the payer

    @Column(name = "payer_email", nullable = false, length = 255)
    private String payerEmail;  // Payer's email

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;  // Payment amount (use BigDecimal for accuracy)

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;  // Currency code (USD, EUR, etc.)

    @Column(name = "payment_time", nullable = false)
    private Instant paymentTime;  // Timestamp when payment was captured

    @Column(name = "transaction_id", nullable = false, unique = true, length = 255)
    private String transactionId;  // Unique PayPal transaction ID

    @Column(name = "approval_url", columnDefinition = "TEXT")
    private String approvalUrl;  // Approval link (optional)

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;  // Store error details if payment fails (nullable)
}
