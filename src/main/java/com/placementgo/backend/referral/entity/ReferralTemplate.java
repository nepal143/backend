package com.placementgo.backend.referral.entity;

import com.placementgo.backend.referral.enums.TemplateType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "referral_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder  // ✅ yeh hona chahiye — builder() yahi banata hai
public class ReferralTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID referralId;

    @Enumerated(EnumType.STRING)
    private TemplateType type;

    @Column(columnDefinition = "TEXT")
    private String message;

    private int version;
}