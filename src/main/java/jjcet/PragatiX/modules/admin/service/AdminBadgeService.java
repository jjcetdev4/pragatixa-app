package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.dto.BadgeCreateUpdateDto;
import jjcet.PragatiX.dto.BadgeDto;
import jjcet.PragatiX.entity.Badge;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.repository.BadgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminBadgeService {

    private static final Logger log = LoggerFactory.getLogger(AdminBadgeService.class);

    private final BadgeRepository badgeRepository;
    private final AuditService auditService;

    public AdminBadgeService(BadgeRepository badgeRepository, AuditService auditService) {
        this.badgeRepository = badgeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BadgeDto> getAllActiveBadges() {
        return badgeRepository.findByDeletedFalse().stream()
                .map(BadgeDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BadgeDto getBadgeById(Long id) {
        Badge badge = badgeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Badge not found with ID: " + id));
        return new BadgeDto(badge);
    }

    @Transactional
    public BadgeDto createBadge(BadgeCreateUpdateDto dto, String username) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Badge name cannot be empty");
        }
        String cleanName = dto.getName().trim();
        if (badgeRepository.existsByNameAndDeletedFalse(cleanName)) {
            throw new IllegalArgumentException("A badge with the name '" + cleanName + "' already exists");
        }

        Badge badge = new Badge();
        badge.setName(cleanName);
        badge.setTier(dto.getTier() != null && !dto.getTier().isBlank() ? dto.getTier().trim() : "ACHIEVEMENT");
        badge.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        badge.setXpRequired(dto.getXpRequired() != null ? dto.getXpRequired() : 0);
        badge.setIconUrl(dto.getIconUrl() != null ? dto.getIconUrl().trim() : "");
        badge.setApprovalAuthority(dto.getApprovalAuthority() != null && !dto.getApprovalAuthority().isBlank()
                ? dto.getApprovalAuthority().trim() : "Admin");
        badge.setRarity(dto.getRarity() != null && !dto.getRarity().isBlank() ? dto.getRarity().trim() : "COMMON");
        badge.setProofRequired(dto.getProofRequired() == null || dto.getProofRequired());
        badge.setDeleted(false);

        Badge saved = badgeRepository.save(badge);

        auditService.log(
                AuditAction.CREATE,
                AuditModule.BADGE,
                "BADGE",
                saved.getId(),
                "Created badge '" + saved.getName() + "'"
        );

        log.info("Badge created successfully with id {} by user {}", saved.getId(), username);
        return new BadgeDto(saved);
    }

    @Transactional
    public BadgeDto updateBadge(Long id, BadgeCreateUpdateDto dto, String username) {
        Badge badge = badgeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Badge not found with ID: " + id));

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Badge name cannot be empty");
        }
        String cleanName = dto.getName().trim();
        if (badgeRepository.existsByNameAndIdNotAndDeletedFalse(cleanName, id)) {
            throw new IllegalArgumentException("Another badge with the name '" + cleanName + "' already exists");
        }

        badge.setName(cleanName);
        if (dto.getTier() != null && !dto.getTier().isBlank()) {
            badge.setTier(dto.getTier().trim());
        }
        if (dto.getDescription() != null) {
            badge.setDescription(dto.getDescription().trim());
        }
        if (dto.getXpRequired() != null) {
            badge.setXpRequired(dto.getXpRequired());
        }
        if (dto.getIconUrl() != null) {
            badge.setIconUrl(dto.getIconUrl().trim());
        }
        if (dto.getApprovalAuthority() != null && !dto.getApprovalAuthority().isBlank()) {
            badge.setApprovalAuthority(dto.getApprovalAuthority().trim());
        }
        if (dto.getRarity() != null && !dto.getRarity().isBlank()) {
            badge.setRarity(dto.getRarity().trim());
        }
        if (dto.getProofRequired() != null) {
            badge.setProofRequired(dto.getProofRequired());
        }

        Badge saved = badgeRepository.save(badge);

        auditService.log(
                AuditAction.UPDATE,
                AuditModule.BADGE,
                "BADGE",
                saved.getId(),
                "Updated badge '" + saved.getName() + "'"
        );

        log.info("Badge id {} updated successfully by user {}", saved.getId(), username);
        return new BadgeDto(saved);
    }

    @Transactional
    public void deleteBadge(Long id, String username) {
        Badge badge = badgeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Badge not found with ID: " + id));

        badge.setDeleted(true);
        badge.setDeletedAt(LocalDateTime.now());
        badge.setPermanentDeleteAt(LocalDateTime.now().plusDays(30));
        badge.setDeletedBy(username);
        badgeRepository.save(badge);

        auditService.log(
                AuditAction.DELETE,
                AuditModule.BADGE,
                "BADGE",
                id,
                "Moved badge '" + badge.getName() + "' to Recycle Bin"
        );

        log.info("Badge id {} soft-deleted and moved to Recycle Bin by user {}", id, username);
    }
}
