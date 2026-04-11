package com.placementgo.backend.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    /** Razorpay order id used to create the payment */
    @Column(nullable = false)
    private String razorpayOrderId;

    /** Razorpay payment id returned after successful payment */
    private String razorpayPaymentId;

    /** PRO / FREE */
    @Column(nullable = false)
    @Builder.Default
    private String plan = "PRO";

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING | ACTIVE | FAILED | EXPIRED

    /** Amount in paise (e.g. 49900 = ₹499) */
    private Integer amountPaise;

    private Instant activatedAt;

    private Instant expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
