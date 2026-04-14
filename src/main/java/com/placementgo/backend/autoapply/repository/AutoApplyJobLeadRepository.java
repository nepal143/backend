package com.placementgo.backend.autoapply.repository;

import com.placementgo.backend.autoapply.entity.AutoApplyJobLead;
import com.placementgo.backend.autoapply.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutoApplyJobLeadRepository extends JpaRepository<AutoApplyJobLead, UUID> {

    Page<AutoApplyJobLead> findByUserIdOrderByDiscoveredAtDesc(UUID userId, Pageable pageable);

    List<AutoApplyJobLead> findByUserIdAndStatus(UUID userId, LeadStatus status);

    Optional<AutoApplyJobLead> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndExternalJobIdAndSource(UUID userId, String externalJobId, String source);

    /** Count how many EMAIL_SENT + AUTO_APPLIED leads a user has in the past 24 h */
    @Query("""
            SELECT COUNT(l) FROM AutoApplyJobLead l
            WHERE l.userId = :userId
              AND l.status IN (
                    com.placementgo.backend.autoapply.enums.LeadStatus.EMAIL_SENT,
                    com.placementgo.backend.autoapply.enums.LeadStatus.AUTO_APPLIED,
                    com.placementgo.backend.autoapply.enums.LeadStatus.MANUALLY_APPLIED
                  )
              AND l.appliedAt >= :since
            """)
    long countAppliedSince(UUID userId, LocalDateTime since);

    @Query("SELECT COUNT(l) FROM AutoApplyJobLead l WHERE l.userId = :userId AND l.status = :status")
    long countByUserIdAndStatus(UUID userId, LeadStatus status);
}
