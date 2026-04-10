package com.placementgo.backend.jobs.repository;

import com.placementgo.backend.jobs.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByUser_Id(UUID userId);
    boolean existsByUser_IdAndJobPosting_Id(UUID userId, UUID jobPostingId);
}
