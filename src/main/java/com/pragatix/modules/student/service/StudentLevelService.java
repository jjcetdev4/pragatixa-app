package com.pragatix.modules.student.service;

import com.pragatix.entity.Level;
import com.pragatix.entity.Student;
import com.pragatix.modules.student.dto.response.StudentProgressionDto;
import com.pragatix.repository.LevelRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentLevelService {

    private final StudentRepository studentRepository;
    private final LevelRepository levelRepository;
    private final com.pragatix.student.XpQueryService xpQueryService;

    public StudentLevelService(StudentRepository studentRepository, LevelRepository levelRepository,
            com.pragatix.student.XpQueryService xpQueryService) {
        this.studentRepository = studentRepository;
        this.levelRepository = levelRepository;
        this.xpQueryService = xpQueryService;
    }

    @Transactional(readOnly = true)
    public StudentProgressionDto getStudentProgression(String username) {
        Student student = studentRepository.findByRegNo(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        int totalXp = student.getTotalXp();
        List<Level> levels = levelRepository.findAllByOrderByXpMinAsc();

        if (levels.isEmpty()) {
            throw new RuntimeException("No levels configured in the system.");
        }

        Level currentLevel = null;
        Level nextLevel = null;

        for (int i = 0; i < levels.size(); i++) {
            Level lvl = levels.get(i);
            if (totalXp >= lvl.getXpMin() && totalXp <= lvl.getXpMax()) {
                currentLevel = lvl;
                if (i + 1 < levels.size()) {
                    nextLevel = levels.get(i + 1);
                }
                break;
            }
        }

        // If XP exceeds all configured levels
        if (currentLevel == null && totalXp > levels.get(levels.size() - 1).getXpMax()) {
            currentLevel = levels.get(levels.size() - 1);
            nextLevel = null;
        }

        // If XP is somehow lower than the first level
        if (currentLevel == null) {
            currentLevel = levels.get(0);
            if (levels.size() > 1) {
                nextLevel = levels.get(1);
            }
        }

        StudentProgressionDto dto = new StudentProgressionDto();
        dto.setTotalXp(totalXp);
        dto.setCurrentLevel(currentLevel.getLevelNumber());
        dto.setCurrentLevelName(currentLevel.getTitle());
        dto.setCurrentLevelMinXp(currentLevel.getXpMin());
        dto.setCurrentLevelMaxXp(currentLevel.getXpMax());

        boolean isMax = (nextLevel == null);
        dto.setIsMaxLevel(isMax);

        if (!isMax) {
            dto.setNextLevel(nextLevel.getLevelNumber());
            dto.setRemainingXp(currentLevel.getXpMax() - totalXp + 1);
            double progress = (double) (totalXp - currentLevel.getXpMin())
                    / (currentLevel.getXpMax() - currentLevel.getXpMin()) * 100.0;
            if (progress < 0)
                progress = 0;
            if (progress > 100)
                progress = 100;
            dto.setProgressPercentage(progress);
        } else {
            dto.setNextLevel(null);
            dto.setRemainingXp(0);
            dto.setProgressPercentage(100.0);
        }

        List<StudentProgressionDto.LevelDto> unlocked = new ArrayList<>();
        List<StudentProgressionDto.LevelDto> locked = new ArrayList<>();

        for (Level lvl : levels) {
            StudentProgressionDto.LevelDto lDto = new StudentProgressionDto.LevelDto(
                    lvl.getLevelNumber(), lvl.getTitle(), lvl.getXpMin(), lvl.getXpMax(),
                    lvl.getStage(), lvl.getPrimaryObjective(), lvl.getKeyUnlocks());
            if (lvl.getLevelNumber() <= currentLevel.getLevelNumber()) {
                unlocked.add(lDto);
            } else {
                locked.add(lDto);
            }
        }

        dto.setUnlockedLevels(unlocked);
        dto.setLockedLevels(locked);

        return dto;
    }
}
