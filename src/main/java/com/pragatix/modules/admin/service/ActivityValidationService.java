package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.repository.CustomFrequencyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ActivityValidationService {

    private final CustomFrequencyRepository customFrequencyRepository;

    public ActivityValidationService(CustomFrequencyRepository customFrequencyRepository) {
        this.customFrequencyRepository = customFrequencyRepository;
    }

    public ResponseEntity<ApiResponse<String>> validateXpCategory(String xpCategory) {
        if (xpCategory == null || xpCategory.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>error("XP Category is required"));
        }
        List<String> allowedCategories = List.of(
                "Academic", "Skill", "Communication", "Leadership", "Discipline",
                "Placement", "Innovation", "Community", "Sports", "Cultural");
        boolean isAllowed = allowedCategories.stream().anyMatch(cat -> cat.equalsIgnoreCase(xpCategory.trim()));
        if (!isAllowed) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>error("Invalid XP Category: " + xpCategory));
        }
        return null; // OK
    }

    public String matchXpCategory(String xpCategory) {
        if (xpCategory == null || xpCategory.trim().isEmpty())
            return xpCategory;
        List<String> allowedCategories = List.of(
                "Academic", "Skill", "Communication", "Leadership", "Discipline",
                "Placement", "Innovation", "Community", "Sports", "Cultural");
        return allowedCategories.stream()
                .filter(cat -> cat.equalsIgnoreCase(xpCategory.trim()))
                .findFirst()
                .orElse(xpCategory);
    }

    public ResponseEntity<ApiResponse<String>> validateXpConfiguration(boolean awardEnabled, boolean penaltyEnabled,
            Integer awardXp, Integer penaltyXp) {
        if (!awardEnabled && !penaltyEnabled) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<String>error("At least one XP Configuration (Award or Penalty) must be enabled"));
        }
        if (awardEnabled) {
            if (awardXp == null || awardXp < 0 || (!penaltyEnabled && awardXp == 0)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<String>error("Award XP value must be greater than zero when enabled"));
            }
        }
        if (penaltyEnabled) {
            if (penaltyXp == null || penaltyXp <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<String>error("Penalty XP value must be greater than zero when enabled"));
            }
        }
        return null; // OK
    }

    public ResponseEntity<ApiResponse<String>> validateAwardFrequency(String awardFrequencyFinal) {
        List<String> validFrequencies = List.of("One Time", "Daily", "Weekly", "Monthly", "Every Period",
                "Per Assignment", "Manual");
        boolean freqValid = validFrequencies.stream().anyMatch(f -> f.equalsIgnoreCase(awardFrequencyFinal));
        if (!freqValid) {
            freqValid = customFrequencyRepository.findByNameIgnoreCase(awardFrequencyFinal).isPresent();
        }
        if (!freqValid) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>error(
                    "Invalid Award Frequency. Must be one of: One Time, Daily, Weekly, Monthly, Every Period, Per Assignment, Manual, or a registered Custom Frequency"));
        }
        return null; // OK
    }

    public String matchAwardFrequency(String awardFrequencyFinal) {
        List<String> validFrequencies = List.of("One Time", "Daily", "Weekly", "Monthly", "Every Period",
                "Per Assignment", "Manual");
        return validFrequencies.stream()
                .filter(f -> f.equalsIgnoreCase(awardFrequencyFinal)).findFirst().orElse(awardFrequencyFinal);
    }

    public ResponseEntity<ApiResponse<String>> validateAwardDays(List<String> awardDays) {
        if (awardDays != null && !awardDays.isEmpty()) {
            List<String> validDays = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
                    "Sunday");
            for (String day : awardDays) {
                if (validDays.stream().noneMatch(d -> d.equalsIgnoreCase(day.trim()))) {
                    return ResponseEntity.badRequest().body(ApiResponse
                            .<String>error("Invalid Award Day: " + day + ". Must be a valid day of the week."));
                }
            }
        }
        return null; // OK
    }

    public ResponseEntity<ApiResponse<String>> validateCap(String matchedFrequency, Integer cap) {
        if (!matchedFrequency.equalsIgnoreCase("One Time") && !matchedFrequency.equalsIgnoreCase("Manual")) {
            if (cap == null || cap < 1) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<String>error("Cap must be at least 1 for recurring frequencies"));
            }
        }
        return null;
    }
}
