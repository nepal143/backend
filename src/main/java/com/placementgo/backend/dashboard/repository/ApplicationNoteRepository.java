package com.placementgo.backend.dashboard.repository;

import com.placementgo.backend.dashboard.entity.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, UUID> {
    List<ApplicationNote> findByApplicationId(UUID applicationId);
}
