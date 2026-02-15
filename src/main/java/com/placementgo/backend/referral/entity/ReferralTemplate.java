package com.placementgo.backend.referral.entity;

import com.placementgo.backend.referral.enums.TemplateType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class ReferralTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private ReferralRequest referralRequest;

    @Enumerated(EnumType.STRING)
    private TemplateType type;

    @Column(length = 2000)
    private String message;

    private int version;
}
