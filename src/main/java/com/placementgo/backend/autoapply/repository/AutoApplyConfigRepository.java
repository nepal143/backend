package com.placementgo.backend.autoapply.repository;

import com.placementgo.backend.autoapply.entity.AutoApplyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AutoApplyConfigRepository extends JpaRepository<AutoApplyConfig, UUID> {
    Optional<AutoApplyConfig> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
