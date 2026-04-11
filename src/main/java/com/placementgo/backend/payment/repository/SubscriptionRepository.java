package com.placementgo.backend.payment.repository;

import com.placementgo.backend.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByUserIdAndStatus(UUID userId, String status);
}
