package com.spdms.entity;

public enum AssignedAcademicYear {
    FIRST_YEAR("First Year"),
    SECOND_YEAR("Second Year"),
    THIRD_YEAR("Third Year"),
    FOURTH_YEAR("Fourth Year");

    private final String displayName;

    AssignedAcademicYear(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
