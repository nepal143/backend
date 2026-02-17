package com.placementgo.backend.jd_intel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interview_insights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String company;
    private String role;

    @Column(columnDefinition = "TEXT")
    private String jobDescription; // Optional, might be long

    @ElementCollection
    private List<String> technicalQuestions;

    @ElementCollection
    private List<String> behavioralQuestions;

    @ElementCollection
    private List<String> codingFocus;

    @ElementCollection
    private List<String> systemDesignFocus;

    @ElementCollection
    private List<String> predictedRounds;

    private String difficultyLevel; // Easy, Medium, Hard

    @ElementCollection
    private List<String> rejectionReasons;

    @ElementCollection
    private List<String> companyTips;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
