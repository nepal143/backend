package com.placementgo.backend.autoapply.enums;

public enum LeadStatus {
    /** Found by the discovery engine, waiting for user action or auto-decision */
    PENDING_REVIEW,
    /** Auto-applied via email (resume sent to apply@ address) */
    EMAIL_SENT,
    /** Fully automated form-fill succeeded */
    AUTO_APPLIED,
    /** Cannot be auto-applied; user notified with template */
    MANUAL_REQUIRED,
    /** User decided to skip this lead */
    SKIPPED,
    /** User manually marked it as applied */
    MANUALLY_APPLIED,
    /** Automation attempt failed */
    FAILED
}
