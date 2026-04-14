package com.placementgo.backend.resume.repository;

import com.placementgo.backend.resume.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    @Query("SELECT r FROM Resume r WHERE r.userId = :userId AND r.generatedPdfBase64 IS NOT NULL ORDER BY r.createdAt DESC")
    List<Resume> findGeneratedByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Resume> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}