package com.spdms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spdms.modules.activity.service.ActivityStageService;
import com.spdms.modules.activity.dto.response.ActivityStageResponse;
import com.spdms.entity.Student;
import com.spdms.modules.student.repository.StudentRepository;
import com.spdms.modules.activity.service.StageValidationService;
import com.spdms.modules.activity.dto.response.StageValidationResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckStages implements CommandLineRunner {

    private final ActivityStageService activityStageService;
    private final StageValidationService stageValidationService;
    private final StudentRepository studentRepository;

    public CheckStages(ActivityStageService activityStageService, StageValidationService stageValidationService, StudentRepository studentRepository) {
        this.activityStageService = activityStageService;
        this.stageValidationService = stageValidationService;
        this.studentRepository = studentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== CHECKING STAGES ======");
        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }
        Student student = students.get(0);
        System.out.println("Using Student ID: " + student.getId());

        List<ActivityStageResponse> stages = activityStageService.getAllStages();
        System.out.println("Database Stage Count -> " + stages.size());

        for (ActivityStageResponse stage : stages) {
            StageValidationResponse validation = stageValidationService.validateStage(student.getId(), stage.getId());
            stage.setValidation(validation);
            stage.setVisible(validation.isVisible());
            stage.setLocked(validation.isLocked());
            stage.setIsCompleted(validation.isCompleted());
            stage.setIsActive(validation.isActive());
        }

        System.out.println("API JSON Response for Student:");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stages);
        System.out.println(json);
        System.out.println("====== DONE CHECKING STAGES ======");
    }
}
