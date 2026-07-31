package com.pragatix.enums;

public enum AcademicYear {
    FIRST_YEAR,
    SECOND_YEAR,
    THIRD_YEAR,
    FOURTH_YEAR;

    public static AcademicYear fromStudent(com.pragatix.entity.Student student) {
        if (student == null) return null;

        // 1. Check YearRef yearNo
        if (student.getYearRef() != null && student.getYearRef().getYearNo() != null) {
            int no = student.getYearRef().getYearNo();
            if (no == 1) return FIRST_YEAR;
            if (no == 2) return SECOND_YEAR;
            if (no == 3) return THIRD_YEAR;
            if (no == 4) return FOURTH_YEAR;
        }

        // 2. Check text sources
        java.util.List<String> sources = new java.util.ArrayList<>();
        if (student.getYearRef() != null && student.getYearRef().getYearName() != null) {
            sources.add(student.getYearRef().getYearName());
        }
        if (student.getYear() != null) {
            sources.add(student.getYear());
        }
        if (student.getAcademicYear() != null) {
            sources.add(student.getAcademicYear());
        }
        if (student.getAcademicYearRef() != null && student.getAcademicYearRef().getAcademicYear() != null) {
            sources.add(student.getAcademicYearRef().getAcademicYear());
        }

        for (String raw : sources) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String s = raw.trim().toUpperCase();

            try {
                return AcademicYear.valueOf(s.replace(" ", "_"));
            } catch (Exception ignored) {}

            if (s.contains("FIRST") || s.equals("1") || s.equals("I") || s.contains("1ST")) return FIRST_YEAR;
            if (s.contains("SECOND") || s.equals("2") || s.equals("II") || s.contains("2ND")) return SECOND_YEAR;
            if (s.contains("THIRD") || s.equals("3") || s.equals("III") || s.contains("3RD")) return THIRD_YEAR;
            if (s.contains("FOURTH") || s.equals("4") || s.equals("IV") || s.contains("4TH")) return FOURTH_YEAR;
        }

        return null;
    }
}
