package com.placementgo.backend.jd_intel.repository;

import com.placementgo.backend.jd_intel.entity.InterviewInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewInsightRepository extends JpaRepository<InterviewInsight, UUID> {
}
