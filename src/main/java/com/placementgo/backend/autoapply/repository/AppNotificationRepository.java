package com.placementgo.backend.autoapply.repository;

import com.placementgo.backend.autoapply.entity.AppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

    Page<AppNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsRead(UUID userId, boolean isRead);

    @Modifying
    @Transactional
    @Query("UPDATE AppNotification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllReadForUser(UUID userId);
}
