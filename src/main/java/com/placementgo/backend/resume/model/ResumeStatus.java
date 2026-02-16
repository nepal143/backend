package com.placementgo.backend.resume.model;

public enum ResumeStatus {

    UPLOADED,

    PARSING,
    PARSED,

    GENERATING_LATEX,
    GENERATED,

    COMPILING_PDF,
    PDF_GENERATED,

    FAILED
}
