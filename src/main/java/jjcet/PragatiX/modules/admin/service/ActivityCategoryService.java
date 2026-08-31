package jjcet.PragatiX.modules.admin.service;

import jakarta.annotation.PostConstruct;
import jjcet.PragatiX.dto.ActivityCategoryCreateUpdateDto;
import jjcet.PragatiX.dto.ActivityCategoryDto;
import jjcet.PragatiX.entity.ActivityCategory;
import jjcet.PragatiX.enums.AuditAction;
import jjcet.PragatiX.enums.AuditModule;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.repository.ActivityCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityCategoryService {

    private static final Logger log = LoggerFactory.getLogger(ActivityCategoryService.class);

    private final ActivityCategoryRepository categoryRepository;
    private final AuditService auditService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public ActivityCategoryService(ActivityCategoryRepository categoryRepository, AuditService auditService) {
        this(categoryRepository, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ActivityCategoryService(ActivityCategoryRepository categoryRepository, AuditService auditService,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public static final List<String> DEFAULT_CATEGORIES = List.of(
            "Academic",
            "Skill",
            "Communication",
            "Leadership",
            "Discipline",
            "Placement",
            "Innovation",
            "Community",
            "Sports",
            "Cultural"
    );

    @PostConstruct
    @Transactional
    public void seedDefaultCategories() {
        try {
            if (jdbcTemplate != null) {
                try {
                    jdbcTemplate.execute("ALTER TABLE activity_categories MODIFY COLUMN activity_name VARCHAR(100) NULL");
                } catch (Exception ignored) {
                }
            }

            int order = 1;
            for (String catName : DEFAULT_CATEGORIES) {
                if (!categoryRepository.existsByNameIgnoreCaseAndDeletedFalse(catName)) {
                    ActivityCategory cat = ActivityCategory.builder()
                            .name(catName)
                            .description(catName + " related activities and events")
                            .displayOrder(order++)
                            .deleted(false)
                            .build();
                    categoryRepository.save(cat);
                    log.info("Seeded default activity category: {}", catName);
                }
            }
        } catch (Exception e) {
            log.warn("Could not seed default categories (may already exist): {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ActivityCategoryDto> getAllActiveCategories() {
        return categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(ActivityCategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getActiveCategoryNames() {
        return categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(ActivityCategory::getName)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActivityCategoryDto getCategoryById(Long id) {
        ActivityCategory category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity Category not found with id: " + id));
        return ActivityCategoryDto.fromEntity(category);
    }

    @Transactional
    public ActivityCategoryDto createCategory(ActivityCategoryCreateUpdateDto dto, String username) {
        String trimmedName = dto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndDeletedFalse(trimmedName)) {
            throw new IllegalArgumentException("Activity Category with name '" + trimmedName + "' already exists");
        }

        ActivityCategory category = ActivityCategory.builder()
                .name(trimmedName)
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .icon(dto.getIcon() != null ? dto.getIcon().trim() : null)
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .deleted(false)
                .build();

        ActivityCategory saved = categoryRepository.save(category);

        auditService.log(
                AuditAction.CREATE,
                AuditModule.ACTIVITY,
                "ACTIVITY_CATEGORY",
                saved.getId(),
                "Created Activity Category: " + saved.getName()
        );

        log.info("Activity Category '{}' (id: {}) created by user {}", saved.getName(), saved.getId(), username);
        return ActivityCategoryDto.fromEntity(saved);
    }

    @Transactional
    public ActivityCategoryDto updateCategory(Long id, ActivityCategoryCreateUpdateDto dto, String username) {
        ActivityCategory category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity Category not found with id: " + id));

        String trimmedName = dto.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNotAndDeletedFalse(trimmedName, id)) {
            throw new IllegalArgumentException("Another Activity Category with name '" + trimmedName + "' already exists");
        }

        String oldName = category.getName();
        category.setName(trimmedName);
        if (dto.getDescription() != null) {
            category.setDescription(dto.getDescription().trim());
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon().trim());
        }
        if (dto.getDisplayOrder() != null) {
            category.setDisplayOrder(dto.getDisplayOrder());
        }

        ActivityCategory updated = categoryRepository.save(category);

        auditService.log(
                AuditAction.UPDATE,
                AuditModule.ACTIVITY,
                "ACTIVITY_CATEGORY",
                updated.getId(),
                "Updated Activity Category from '" + oldName + "' to '" + updated.getName() + "'"
        );

        log.info("Activity Category id: {} updated by user {}", id, username);
        return ActivityCategoryDto.fromEntity(updated);
    }

    @Transactional
    public void deleteCategory(Long id, String username) {
        ActivityCategory category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity Category not found with id: " + id));

        category.setDeleted(true);
        category.setDeletedAt(LocalDateTime.now());
        category.setPermanentDeleteAt(LocalDateTime.now().plusDays(30));
        category.setDeletedBy(username);

        categoryRepository.save(category);

        auditService.log(
                AuditAction.DELETE,
                AuditModule.ACTIVITY,
                "ACTIVITY_CATEGORY",
                category.getId(),
                "Moved Activity Category '" + category.getName() + "' to Recycle Bin"
        );

        log.info("Activity Category '{}' (id: {}) moved to Recycle Bin by user {}", category.getName(), id, username);
    }
}
