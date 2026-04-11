package com.placementgo.backend.payment.controller;

import com.placementgo.backend.payment.dto.CreateOrderResponse;
import com.placementgo.backend.payment.dto.SubscriptionStatusResponse;
import com.placementgo.backend.payment.dto.VerifyPaymentRequest;
import com.placementgo.backend.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Create a new Razorpay order for PRO subscription */
    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @AuthenticationPrincipal UUID userId) {
        try {
            CreateOrderResponse response = paymentService.createOrder(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Verify payment signature and activate subscription */
    @PostMapping("/verify")
    public ResponseEntity<SubscriptionStatusResponse> verify(
            @AuthenticationPrincipal UUID userId,
            @RequestBody VerifyPaymentRequest request) {
        try {
            SubscriptionStatusResponse response = paymentService.verifyAndActivate(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Get current subscription status */
    @GetMapping("/status")
    public ResponseEntity<SubscriptionStatusResponse> status(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(paymentService.getStatus(userId));
    }
}
