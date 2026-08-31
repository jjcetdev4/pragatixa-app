package jjcet.PragatiX.modules.admin.service;

import jakarta.annotation.PostConstruct;
import jjcet.PragatiX.dto.ActivityEvidenceCreateUpdateDto;
import jjcet.PragatiX.dto.ActivityEvidenceDto;
import jjcet.PragatiX.entity.ActivityEvidence;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.repository.ActivityEvidenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(ActivityEvidenceService.class);

    private final ActivityEvidenceRepository evidenceRepository;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    public ActivityEvidenceService(ActivityEvidenceRepository evidenceRepository, AuditService auditService) {
        this(evidenceRepository, auditService, null);
    }

    @Autowired
    public ActivityEvidenceService(ActivityEvidenceRepository evidenceRepository, AuditService auditService,
                                   @Autowired(required = false) JdbcTemplate jdbcTemplate) {
        this.evidenceRepository = evidenceRepository;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public static final List<String> DEFAULT_EVIDENCES = List.of(
            "Handwritten",
            "Soft Copy",
            "Diary / Notebook",
            "Weekly Log",
            "Direct Observation",
            "Attendance Register",
            "ERP Attendance"
    );

    @PostConstruct
    @Transactional
    public void seedDefaultEvidences() {
        try {
            // Clean up legacy 'Manual' if present
            try {
                evidenceRepository.findByNameIgnoreCaseAndDeletedFalse("Manual")
                        .ifPresent(evidenceRepository::delete);
            } catch (Exception ignored) {}

            int order = 1;
            for (String evName : DEFAULT_EVIDENCES) {
                if (!evidenceRepository.existsByNameIgnoreCaseAndDeletedFalse(evName)) {
                    ActivityEvidence ev = ActivityEvidence.builder()
                            .name(evName)
                            .description(evName + " verification evidence")
                            .displayOrder(order++)
                            .deleted(false)
                            .build();
                    evidenceRepository.save(ev);
                    log.info("Seeded default activity evidence: {}", evName);
                }
            }
        } catch (Exception e) {
            log.warn("Could not seed default evidences: {}", e.getMessage());
        }
    }

    public List<ActivityEvidenceDto> getAllActiveEvidences() {
        List<ActivityEvidence> list = evidenceRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc();
        if (list.isEmpty()) {
            return DEFAULT_EVIDENCES.stream()
                    .map(name -> new ActivityEvidenceDto(null, name, name, 0, null, null))
                    .collect(Collectors.toList());
        }
        return list.stream().map(ActivityEvidenceDto::fromEntity).collect(Collectors.toList());
    }

    public List<String> getAllActiveEvidenceNames() {
        List<ActivityEvidence> list = evidenceRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc();
        if (list.isEmpty()) {
            return DEFAULT_EVIDENCES;
        }
        return list.stream().map(ActivityEvidence::getName).collect(Collectors.toList());
    }

    public ActivityEvidenceDto getEvidenceById(Long id) {
        ActivityEvidence ev = evidenceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity Evidence not found with id: " + id));
        return ActivityEvidenceDto.fromEntity(ev);
    }

    @Transactional
    public ActivityEvidenceDto createEvidence(ActivityEvidenceCreateUpdateDto dto, String username) {
        String trimmedName = dto.getName().trim();
        if (evidenceRepository.existsByNameIgnoreCaseAndDeletedFalse(trimmedName)) {
            throw new IllegalArgumentException("Activity Evidence with name '" + trimmedName + "' already exists");
        }

        ActivityEvidence ev = ActivityEvidence.builder()
                .name(trimmedName)
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .deleted(false)
                .build();

        ActivityEvidence saved = evidenceRepository.save(ev);

        auditService.log(
                AuditAction.CREATE,
                AuditModule.ACTIVITY,
                "ACTIVITY_EVIDENCE",
                saved.getId(),
                "Created Activity Evidence: " + saved.getName()
        );

        log.info("Activity Evidence '{}' (id: {}) created by user {}", saved.getName(), saved.getId(), username);
        return ActivityEvidenceDto.fromEntity(saved);
    }

    @Transactional
    public ActivityEvidenceDto updateEvidence(Long id, ActivityEvidenceCreateUpdateDto dto, String username) {
        ActivityEvidence ev = evidenceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity Evidence not found with id: " + id));

        String trimmedName = dto.getName().trim();
        if (evidenceRepository.existsByNameIgnoreCaseAndIdNotAndDeletedFalse(trimmedName, id)) {
            throw new IllegalArgumentException("Another Activity Evidence with name '" + trimmedName + "' already exists");
        }

        String oldName = ev.getName();
        ev.setName(trimmedName);
        if (dto.getDescription() != null) {
            ev.setDescription(dto.getDescription().trim());
        }
        if (dto.getDisplayOrder() != null) {
            ev.setDisplayOrder(dto.getDisplayOrder());
        }

        ActivityEvidence updated = evidenceRepository.save(ev);

        // Update activities using old evidence name in comma-separated list if renamed
        if (jdbcTemplate != null && !oldName.equalsIgnoreCase(trimmedName)) {
            try {
                jdbcTemplate.update(
                        "UPDATE activities SET evidence = REPLACE(evidence, ?, ?) WHERE evidence LIKE ?",
                        oldName, trimmedName, "%" + oldName + "%"
                );
            } catch (Exception e) {
                log.warn("Could not update evidence references in activities: {}", e.getMessage());
            }
        }

        auditService.log(
                AuditAction.UPDATE,
                AuditModule.ACTIVITY,
                "ACTIVITY_EVIDENCE",
                updated.getId(),
                "Updated Activity Evidence from '" + oldName + "' to '" + updated.getName() + "'"
        );

        log.info("Activity Evidence id: {} updated by user {}", id, username);
        return ActivityEvidenceDto.fromEntity(updated);
    }

    @Transactional
    public void deleteEvidence(Long id, String username) {
        ActivityEvidence ev = evidenceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity Evidence not found with id: " + id));

        ev.setDeleted(true);
        ev.setDeletedAt(LocalDateTime.now());
        ev.setPermanentDeleteAt(LocalDateTime.now().plusDays(30));
        ev.setDeletedBy(username);

        evidenceRepository.save(ev);

        auditService.log(
                AuditAction.DELETE,
                AuditModule.ACTIVITY,
                "ACTIVITY_EVIDENCE",
                id,
                "Moved Activity Evidence '" + ev.getName() + "' to Recycle Bin"
        );

        log.info("Activity Evidence '{}' (id: {}) moved to Recycle Bin by user {}", ev.getName(), id, username);
    }
}
