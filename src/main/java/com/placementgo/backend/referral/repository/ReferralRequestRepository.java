package com.placementgo.backend.referral.repository;

import com.placementgo.backend.referral.entity.ReferralRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRequestRepository
        extends JpaRepository<ReferralRequest, UUID> {

    List<ReferralRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ReferralRequest> findByShareToken(String token);
}
