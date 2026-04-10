package com.placementgo.backend.jobs.repository;

import com.placementgo.backend.jobs.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {
    Optional<JobPosting> findByPlatformJobIdAndJobPlatformSource(String platformJobId, String jobPlatformSource);
}
