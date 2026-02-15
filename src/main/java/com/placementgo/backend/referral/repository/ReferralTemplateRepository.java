package com.placementgo.backend.referral.repository;

import com.placementgo.backend.referral.entity.ReferralTemplate;
import com.placementgo.backend.referral.enums.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferralTemplateRepository
        extends JpaRepository<ReferralTemplate, UUID> {

    List<ReferralTemplate> findByReferralRequestId(UUID referralId);

    long countByReferralRequestIdAndType(UUID referralId, TemplateType type);
}
