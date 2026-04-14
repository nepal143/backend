package com.placementgo.backend.autoapply.enums;

public enum ApplyMethod {
    /** Job board provides a direct email to send application to */
    EMAIL,
    /** Simple hosted form we can describe step-by-step for the user */
    EASY_APPLY_FORM,
    /** Requires full external site navigation – manual only */
    EXTERNAL_FORM,
    UNKNOWN
}
