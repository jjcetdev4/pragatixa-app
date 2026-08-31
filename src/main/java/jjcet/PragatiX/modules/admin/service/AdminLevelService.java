package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.entity.Level;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.enums.AcademicYear;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.modules.admin.dto.request.LevelCreateUpdateDto;
import jjcet.PragatiX.modules.admin.dto.response.LevelDto;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.repository.LevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminLevelService {

    private final LevelRepository levelRepository;
    private final AuthUtils authUtils;
    private final AuditService auditService;

    public AdminLevelService(LevelRepository levelRepository, AuthUtils authUtils, AuditService auditService) {
        this.levelRepository = levelRepository;
        this.authUtils = authUtils;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LevelDto> getLevels(String yearParam) {
        User currentUser = authUtils.getCurrentUser();
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);

        AcademicYear targetYear = null;

        if (!isSuperAdmin && currentUser != null) {
            targetYear = currentUser.getAcademicYear() != null
                    ? currentUser.getAcademicYear()
                    : AcademicYear.fromString(currentUser.getYear());
        } else if (yearParam != null && !yearParam.trim().isEmpty() && !yearParam.equalsIgnoreCase("ALL")) {
            targetYear = AcademicYear.fromString(yearParam);
        }

        List<Level> levels;
        if (targetYear != null) {
            levels = levelRepository.findByAcademicYearAndDeletedFalseOrderByXpMinAsc(targetYear);
            // If year has no specific levels configured, also include template/null year levels if empty
            if (levels.isEmpty()) {
                levels = levelRepository.findByDeletedFalseOrderByXpMinAsc().stream()
                        .filter(l -> l.getAcademicYear() == null)
                        .collect(Collectors.toList());
            }
        } else {
            levels = levelRepository.findByDeletedFalseOrderByXpMinAsc();
        }

        return levels.stream().map(LevelDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LevelDto getLevel(Long id) {
        Level level = levelRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Level not found with ID: " + id));
        return LevelDto.fromEntity(level);
    }

    public LevelDto createLevel(LevelCreateUpdateDto dto) {
        User currentUser = authUtils.getCurrentUser();
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);

        if (dto.getXpMin() >= dto.getXpMax()) {
            throw new IllegalArgumentException("Minimum XP must be less than Maximum XP.");
        }

        AcademicYear academicYear;
        if (!isSuperAdmin && currentUser != null) {
            academicYear = currentUser.getAcademicYear() != null
                    ? currentUser.getAcademicYear()
                    : AcademicYear.fromString(currentUser.getYear());
        } else if (dto.getAcademicYear() != null && !dto.getAcademicYear().trim().isEmpty()) {
            academicYear = AcademicYear.fromString(dto.getAcademicYear());
        } else {
            academicYear = AcademicYear.FIRST_YEAR;
        }

        Level level = Level.builder()
                .levelNumber(dto.getLevelNumber())
                .title(dto.getTitle().trim())
                .xpMin(dto.getXpMin())
                .xpMax(dto.getXpMax())
                .stage(dto.getStage())
                .primaryObjective(dto.getPrimaryObjective())
                .keyUnlocks(dto.getKeyUnlocks())
                .academicYear(academicYear)
                .deleted(false)
                .build();

        Level saved = levelRepository.save(level);

        auditService.log(
                AuditAction.CREATE,
                AuditModule.LEVEL,
                "LEVEL",
                saved.getId(),
                "Created Level " + saved.getLevelNumber() + " (" + saved.getTitle() + ") for year " + saved.getAcademicYear()
        );

        return LevelDto.fromEntity(saved);
    }

    public LevelDto updateLevel(Long id, LevelCreateUpdateDto dto) {
        User currentUser = authUtils.getCurrentUser();
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);

        Level level = levelRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Level not found with ID: " + id));

        if (!isSuperAdmin && currentUser != null) {
            AcademicYear adminYear = currentUser.getAcademicYear() != null
                    ? currentUser.getAcademicYear()
                    : AcademicYear.fromString(currentUser.getYear());
            if (level.getAcademicYear() != null && !level.getAcademicYear().equals(adminYear)) {
                throw new org.springframework.security.access.AccessDeniedException("You can only edit levels for your assigned year.");
            }
        }

        if (dto.getXpMin() >= dto.getXpMax()) {
            throw new IllegalArgumentException("Minimum XP must be less than Maximum XP.");
        }

        level.setLevelNumber(dto.getLevelNumber());
        level.setTitle(dto.getTitle().trim());
        level.setXpMin(dto.getXpMin());
        level.setXpMax(dto.getXpMax());
        level.setStage(dto.getStage());
        level.setPrimaryObjective(dto.getPrimaryObjective());
        level.setKeyUnlocks(dto.getKeyUnlocks());

        if (isSuperAdmin && dto.getAcademicYear() != null && !dto.getAcademicYear().trim().isEmpty()) {
            level.setAcademicYear(AcademicYear.fromString(dto.getAcademicYear()));
        }

        Level updated = levelRepository.save(level);

        auditService.log(
                AuditAction.UPDATE,
                AuditModule.LEVEL,
                "LEVEL",
                updated.getId(),
                "Updated Level " + updated.getLevelNumber() + " (" + updated.getTitle() + ")"
        );

        return LevelDto.fromEntity(updated);
    }

    public void deleteLevel(Long id) {
        User currentUser = authUtils.getCurrentUser();
        boolean isSuperAdmin = currentUser != null && authUtils.isSuperAdmin(currentUser);

        Level level = levelRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Level not found with ID: " + id));

        if (!isSuperAdmin && currentUser != null) {
            AcademicYear adminYear = currentUser.getAcademicYear() != null
                    ? currentUser.getAcademicYear()
                    : AcademicYear.fromString(currentUser.getYear());
            if (level.getAcademicYear() != null && !level.getAcademicYear().equals(adminYear)) {
                throw new org.springframework.security.access.AccessDeniedException("You can only delete levels for your assigned year.");
            }
        }

        level.setDeleted(true);
        level.setDeletedAt(LocalDateTime.now());
        level.setPermanentDeleteAt(LocalDateTime.now().plusDays(30));
        level.setDeletedBy(currentUser != null ? currentUser.getUsername() : "System");
        levelRepository.save(level);

        auditService.log(
                AuditAction.DELETE,
                AuditModule.LEVEL,
                "LEVEL",
                level.getId(),
                "Soft deleted Level " + level.getLevelNumber() + " (" + level.getTitle() + ") to recycle bin"
        );
    }
}
