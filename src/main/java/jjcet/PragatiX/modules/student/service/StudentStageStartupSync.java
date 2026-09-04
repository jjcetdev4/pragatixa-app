package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.entity.ActivityStage;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.enums.AcademicYear;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StudentStageStartupSync {

    private static final Logger log = LoggerFactory.getLogger(StudentStageStartupSync.class);

    private final StudentRepository studentRepository;
    private final ActivityStageRepository activityStageRepository;

    public StudentStageStartupSync(StudentRepository studentRepository,
                                  ActivityStageRepository activityStageRepository) {
        this.studentRepository = studentRepository;
        this.activityStageRepository = activityStageRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncUnassignedStudentStages() {
        try {
            List<Student> unassignedStudents = studentRepository.findAll().stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> s.getStage() <= 0 || s.getCurrentStage() <= 0)
                    .collect(Collectors.toList());

            if (unassignedStudents.isEmpty()) {
                log.info("StudentStageStartupSync: All active students are properly enrolled in valid stages.");
                return;
            }

            log.info("StudentStageStartupSync: Found {} students with unassigned/zero stage. Auto-enrolling into Stage 1...",
                    unassignedStudents.size());

            int updatedCount = 0;
            for (Student student : unassignedStudents) {
                AcademicYear yearEnum = resolveAcademicYear(student);
                ActivityStage stage1 = activityStageRepository
                        .findByAcademicYearAndDisplayOrderAndDeletedFalse(yearEnum, 1)
                        .orElse(null);

                int targetStageOrder = (stage1 != null && stage1.getDisplayOrder() > 0)
                        ? stage1.getDisplayOrder() : 1;
                Long targetStageId = stage1 != null ? stage1.getId() : null;

                student.setStage(targetStageOrder);
                student.setCurrentStage(targetStageOrder);
                student.setCurrentStageId(targetStageId);
                updatedCount++;
            }

            studentRepository.saveAll(unassignedStudents);
            log.info("StudentStageStartupSync: Successfully auto-enrolled {} students into Stage 1.", updatedCount);
        } catch (Exception e) {
            log.error("StudentStageStartupSync: Error syncing student stages on startup: {}", e.getMessage(), e);
        }
    }

    private AcademicYear resolveAcademicYear(Student s) {
        if (s.getYearRef() != null && s.getYearRef().getYearNo() != null) {
            int no = s.getYearRef().getYearNo().intValue();
            if (no == 1) return AcademicYear.FIRST_YEAR;
            if (no == 2) return AcademicYear.SECOND_YEAR;
            if (no == 3) return AcademicYear.THIRD_YEAR;
            if (no == 4) return AcademicYear.FOURTH_YEAR;
        }
        if (s.getYear() != null) {
            try {
                int no = Integer.parseInt(s.getYear().trim());
                if (no == 1) return AcademicYear.FIRST_YEAR;
                if (no == 2) return AcademicYear.SECOND_YEAR;
                if (no == 3) return AcademicYear.THIRD_YEAR;
                if (no == 4) return AcademicYear.FOURTH_YEAR;
            } catch (Exception ignored) {
            }
        }
        return AcademicYear.FIRST_YEAR;
    }
}
