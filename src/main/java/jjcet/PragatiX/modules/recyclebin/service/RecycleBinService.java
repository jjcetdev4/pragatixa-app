package jjcet.PragatiX.modules.recyclebin.service;

import jjcet.PragatiX.modules.recyclebin.dto.RecycleBinItem;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class RecycleBinService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ActivityRepository activityRepository;
    private final TeamRepository teamRepository;
    private final EntityManager entityManager;
    private final jjcet.PragatiX.modules.audit.service.AuditService auditService;
    private final jjcet.PragatiX.modules.admin.service.AdminDepartmentCommandService adminDepartmentCommandService;

    public RecycleBinService(UserRepository userRepository, StudentRepository studentRepository, ActivityRepository activityRepository, TeamRepository teamRepository, EntityManager entityManager, jjcet.PragatiX.modules.audit.service.AuditService auditService, jjcet.PragatiX.modules.admin.service.AdminDepartmentCommandService adminDepartmentCommandService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
        this.entityManager = entityManager;
        this.auditService = auditService;
        this.adminDepartmentCommandService = adminDepartmentCommandService;
    }

    @Transactional(readOnly = true)
    public List<RecycleBinItem> getDeletedItems() {
        List<RecycleBinItem> items = new ArrayList<>();

        // Fetch deleted Users
        List<User> deletedUsers = entityManager.createQuery("SELECT u FROM User u WHERE u.deleted = true", User.class).getResultList();
        items.addAll(deletedUsers.stream().map(u -> new RecycleBinItem(
                u.getId(),
                "USER",
                u.getUsername() + " (" + u.getFullName() + ")",
                u.getDeletedAt(),
                u.getPermanentDeleteAt(),
                u.getDeletedBy(),
                "Dept: " + (u.getDepartment() != null ? (u.getDepartment().getDeptCode() != null ? u.getDepartment().getDeptCode() : u.getDepartment().getName()) : "Global") + " • " + (u.getRoles() != null ? u.getRoles().stream().map(r -> r.getName() != null ? r.getName().replace("ROLE_", "") : "").collect(Collectors.joining(", ")) : "User"),
                "Email: " + u.getEmail() + (u.getPhone() != null ? " • Phone: " + u.getPhone() : "")
        )).collect(Collectors.toList()));

        // Fetch deleted Faculty
        List<Faculty> deletedFaculty = entityManager.createQuery("SELECT f FROM Faculty f WHERE f.deleted = true", Faculty.class).getResultList();
        items.addAll(deletedFaculty.stream().map(f -> new RecycleBinItem(
                f.getId(),
                "FACULTY",
                f.getUser() != null ? f.getUser().getUsername() + " (" + f.getUser().getFullName() + ")" : "Unknown Faculty",
                f.getDeletedAt(),
                f.getPermanentDeleteAt(),
                f.getDeletedBy(),
                "Dept: " + (f.getDepartment() != null ? f.getDepartment().getDeptCode() : "N/A"),
                "Designation: " + (f.getDesignation() != null ? f.getDesignation() : "Faculty")
        )).collect(Collectors.toList()));

        // Fetch deleted Students
        List<Student> deletedStudents = entityManager.createQuery("SELECT s FROM Student s WHERE s.deleted = true", Student.class).getResultList();
        items.addAll(deletedStudents.stream().map(s -> new RecycleBinItem(
                s.getId(),
                "STUDENT",
                s.getFullName() + " (" + s.getRegNo() + ")",
                s.getDeletedAt(),
                s.getPermanentDeleteAt(),
                s.getDeletedBy(),
                "Dept: " + (s.getDepartment() != null ? (s.getDepartment().getDeptCode() != null ? s.getDepartment().getDeptCode() : s.getDepartment().getName()) : "N/A") + " • Year " + (s.getYear() != null ? s.getYear() : "N/A") + " • Sec " + (s.getSection() != null ? s.getSection().getSectionName() : "N/A"),
                "Reg No: " + s.getRegNo() + (s.getSprNo() != null ? " • SPR: " + s.getSprNo() : "") + " • XP: " + s.getTotalXp()
        )).collect(Collectors.toList()));

        // Fetch deleted Activities
        List<Activity> deletedActivities = entityManager.createQuery("SELECT a FROM Activity a WHERE a.deleted = true", Activity.class).getResultList();
        items.addAll(deletedActivities.stream().map(a -> new RecycleBinItem(
                a.getId(),
                "ACTIVITY",
                a.getName(),
                a.getDeletedAt(),
                a.getPermanentDeleteAt(),
                a.getDeletedBy(),
                "Category: " + (a.getActivityCategory() != null ? a.getActivityCategory().getName() : "General") + " • Freq: " + (a.getFrequency() != null ? a.getFrequency() : "N/A"),
                (a.getActivityDescription() != null && !a.getActivityDescription().isBlank()) ? a.getActivityDescription() : ("Max Points: " + a.getMaxPoints())
        )).collect(Collectors.toList()));

        // Fetch deleted Teams
        List<Team> deletedTeams = entityManager.createQuery("SELECT t FROM Team t WHERE t.deleted = true", Team.class).getResultList();
        items.addAll(deletedTeams.stream().map(t -> new RecycleBinItem(
                t.getId(),
                "TEAM",
                t.getName(),
                t.getDeletedAt(),
                t.getPermanentDeleteAt(),
                t.getDeletedBy(),
                "Dept: " + (t.getDepartment() != null ? (t.getDepartment().getDeptCode() != null ? t.getDepartment().getDeptCode() : t.getDepartment().getName()) : "N/A") + " • Year " + (t.getYear() != null ? t.getYear() : "N/A") + " • Sec " + (t.getSection() != null ? t.getSection().getSectionName() : "N/A"),
                "Captain: " + (t.getCaptain() != null ? t.getCaptain().getFullName() : "None") + " • Size: " + t.getSize()
        )).collect(Collectors.toList()));

        // Fetch deleted Departments
        List<Department> deletedDepartments = entityManager.createQuery("SELECT d FROM Department d WHERE d.deleted = true", Department.class).getResultList();
        items.addAll(deletedDepartments.stream().map(d -> new RecycleBinItem(
                d.getId(),
                "DEPARTMENT",
                d.getDeptCode() + " - " + d.getName(),
                d.getDeletedAt(),
                d.getPermanentDeleteAt(),
                d.getDeletedBy(),
                "Academic Structure / Department Master",
                "Dept Code: " + d.getDeptCode() + " • Name: " + d.getName()
        )).collect(Collectors.toList()));

        // Fetch deleted Badges
        List<Badge> deletedBadges = entityManager.createQuery("SELECT b FROM Badge b WHERE b.deleted = true", Badge.class).getResultList();
        items.addAll(deletedBadges.stream().map(b -> new RecycleBinItem(
                b.getId(),
                "BADGE",
                b.getName(),
                b.getDeletedAt(),
                b.getPermanentDeleteAt(),
                b.getDeletedBy(),
                "Gamification / Badges Module",
                (b.getDescription() != null && !b.getDescription().isBlank()) ? b.getDescription() : ("XP Required: " + b.getXpRequired() + " • Tier: " + b.getTier())
        )).collect(Collectors.toList()));

        // Fetch deleted Activity Categories
        List<ActivityCategory> deletedCategories = entityManager.createQuery("SELECT c FROM ActivityCategory c WHERE c.deleted = true", ActivityCategory.class).getResultList();
        items.addAll(deletedCategories.stream().map(c -> new RecycleBinItem(
                c.getId(),
                "ACTIVITY_CATEGORY",
                c.getName(),
                c.getDeletedAt(),
                c.getPermanentDeleteAt(),
                c.getDeletedBy(),
                "Activity Management → Categories Master",
                (c.getDescription() != null && !c.getDescription().isBlank()) ? c.getDescription() : "Standard Activity Category"
        )).collect(Collectors.toList()));

        // Fetch deleted Activity Evidences
        List<ActivityEvidence> deletedEvidences = entityManager.createQuery("SELECT e FROM ActivityEvidence e WHERE e.deleted = true", ActivityEvidence.class).getResultList();
        items.addAll(deletedEvidences.stream().map(e -> new RecycleBinItem(
                e.getId(),
                "ACTIVITY_EVIDENCE",
                e.getName(),
                e.getDeletedAt(),
                e.getPermanentDeleteAt(),
                e.getDeletedBy(),
                "Activity Management → Evidence Types",
                (e.getDescription() != null && !e.getDescription().isBlank()) ? e.getDescription() : "Verification Proof Requirement"
        )).collect(Collectors.toList()));

        // Fetch deleted Levels
        List<Level> deletedLevels = entityManager.createQuery("SELECT l FROM Level l WHERE l.deleted = true", Level.class).getResultList();
        items.addAll(deletedLevels.stream().map(l -> new RecycleBinItem(
                l.getId(),
                "LEVEL",
                "Level " + l.getLevelNumber() + ": " + l.getTitle() + (l.getAcademicYear() != null ? " (" + l.getAcademicYear().name() + ")" : ""),
                l.getDeletedAt(),
                l.getPermanentDeleteAt(),
                l.getDeletedBy(),
                "Level Roadmap • " + (l.getAcademicYear() != null ? l.getAcademicYear().name() : "All Years"),
                (l.getPrimaryObjective() != null && !l.getPrimaryObjective().isBlank()) ? l.getPrimaryObjective() : ("Level " + l.getLevelNumber() + " • XP: " + l.getXpMin() + " - " + l.getXpMax())
        )).collect(Collectors.toList()));

        return items;
    }

    @Transactional
    public void restoreItem(String entityType, Long id) {
        switch (entityType.toUpperCase()) {
            case "USER":
                User user = entityManager.find(User.class, id);
                if (user != null) {
                    user.setDeleted(false);
                    user.setActive(true);
                    user.setDeletedAt(null);
                    user.setPermanentDeleteAt(null);
                    user.setDeletedBy(null);
                    entityManager.merge(user);
                }
                break;
            case "FACULTY":
                Faculty faculty = entityManager.find(Faculty.class, id);
                if (faculty != null) {
                    faculty.setDeleted(false);
                    faculty.setDeletedAt(null);
                    faculty.setPermanentDeleteAt(null);
                    faculty.setDeletedBy(null);
                    entityManager.merge(faculty);
                }
                break;
            case "STUDENT":
                Student student = entityManager.find(Student.class, id);
                if (student != null) {
                    student.setDeleted(false);
                    student.setActive(true);
                    student.setDeletedAt(null);
                    student.setPermanentDeleteAt(null);
                    student.setDeletedBy(null);
                    entityManager.merge(student);
                }
                break;
            case "ACTIVITY":
                Activity activity = entityManager.find(Activity.class, id);
                if (activity != null) {
                    activity.setDeleted(false);
                    activity.setDeletedAt(null);
                    activity.setPermanentDeleteAt(null);
                    activity.setDeletedBy(null);
                    entityManager.merge(activity);
                }
                break;
            case "TEAM":
                Team team = entityManager.find(Team.class, id);
                if (team != null) {
                    team.setDeleted(false);
                    team.setDeletedAt(null);
                    team.setPermanentDeleteAt(null);
                    team.setDeletedBy(null);
                    if (team.getCaptain() != null) {
                        Student captain = entityManager.find(Student.class, team.getCaptain().getId());
                        if (captain != null && captain.getTeam() == null && !captain.isDeleted()) {
                            captain.setTeam(team);
                            entityManager.merge(captain);
                        }
                    }
                    if (team.getViceCaptain() != null) {
                        Student vc = entityManager.find(Student.class, team.getViceCaptain().getId());
                        if (vc != null && vc.getTeam() == null && !vc.isDeleted()) {
                            vc.setTeam(team);
                            entityManager.merge(vc);
                        }
                    }
                    entityManager.merge(team);
                }
                break;
            case "DEPARTMENT":
                Department department = entityManager.find(Department.class, id);
                if (department != null) {
                    department.setDeleted(false);
                    department.setDeletedAt(null);
                    department.setPermanentDeleteAt(null);
                    department.setDeletedBy(null);
                    entityManager.merge(department);
                }
                break;
            case "BADGE":
                Badge badge = entityManager.find(Badge.class, id);
                if (badge != null) {
                    badge.setDeleted(false);
                    badge.setDeletedAt(null);
                    badge.setPermanentDeleteAt(null);
                    badge.setDeletedBy(null);
                    entityManager.merge(badge);
                }
                break;
            case "ACTIVITY_CATEGORY":
            case "CATEGORY":
                ActivityCategory activityCategory = entityManager.find(ActivityCategory.class, id);
                if (activityCategory != null) {
                    activityCategory.setDeleted(false);
                    activityCategory.setDeletedAt(null);
                    activityCategory.setPermanentDeleteAt(null);
                    activityCategory.setDeletedBy(null);
                    entityManager.merge(activityCategory);
                }
                break;
            case "ACTIVITY_EVIDENCE":
            case "EVIDENCE":
                ActivityEvidence activityEvidence = entityManager.find(ActivityEvidence.class, id);
                if (activityEvidence != null) {
                    activityEvidence.setDeleted(false);
                    activityEvidence.setDeletedAt(null);
                    activityEvidence.setPermanentDeleteAt(null);
                    activityEvidence.setDeletedBy(null);
                    entityManager.merge(activityEvidence);
                }
                break;
            case "LEVEL":
                Level level = entityManager.find(Level.class, id);
                if (level != null) {
                    level.setDeleted(false);
                    level.setDeletedAt(null);
                    level.setPermanentDeleteAt(null);
                    level.setDeletedBy(null);
                    entityManager.merge(level);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }
        String restoreModuleStr = entityType.toUpperCase();
        if (restoreModuleStr.equals("ACTIVITY_CATEGORY") || restoreModuleStr.equals("CATEGORY")
                || restoreModuleStr.equals("ACTIVITY_EVIDENCE") || restoreModuleStr.equals("EVIDENCE")) {
            restoreModuleStr = "ACTIVITY";
        }
        auditService.log(
            jjcet.PragatiX.enums.AuditAction.RESTORE,
            jjcet.PragatiX.enums.AuditModule.valueOf(restoreModuleStr),
            entityType,
            id,
            "Restored " + entityType.toLowerCase() + " from Recycle Bin"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void permanentlyDeleteItem(String entityType, Long id) {
        try {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            permanentlyDeleteItemInternal(entityType, id);
        } finally {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }
    }

    private void permanentlyDeleteItemInternal(String entityType, Long id) {
        String actor = "SYSTEM";
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            actor = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        }

        switch (entityType.toUpperCase()) {
            case "USER":
                User user = entityManager.find(User.class, id);
                if (user != null) {
                    // 1. Delete associated Faculty to handle NOT NULL constraints correctly
                    List<?> facultyIds = entityManager.createNativeQuery("SELECT id FROM faculty WHERE user_id = :id").setParameter("id", id).getResultList();
                    for (Object facIdObj : facultyIds) {
                        if (facIdObj instanceof Number) {
                            permanentlyDeleteItemInternal("FACULTY", ((Number) facIdObj).longValue());
                        }
                    }

                    // 2. Clear Nullable References
                    entityManager.createNativeQuery("UPDATE activity_subgroups SET assigned_faculty_id = NULL WHERE assigned_faculty_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("UPDATE students SET user_id = NULL WHERE user_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("UPDATE teams SET created_by_id = NULL WHERE created_by_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("UPDATE activity_completion_requests SET cc_id = NULL WHERE cc_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("UPDATE discipline_logs SET recorded_by_id = NULL WHERE recorded_by_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("UPDATE penalty_requests SET cc_id = NULL WHERE cc_id = :id").setParameter("id", id).executeUpdate();

                    // 3. Delete Owned Child Entities and Mappings (NOT NULL Foreign Keys)
                    entityManager.createNativeQuery("DELETE FROM penalty_requests WHERE teacher_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM student_activity_xp WHERE teacher_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM timetable_entries WHERE faculty_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM timetable WHERE created_by_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_assignments WHERE teacher_id = :id OR assigned_by_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_temporary_assignments WHERE assigned_by_id = :id OR original_teacher_id = :id OR temporary_teacher_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM user_sub_roles WHERE user_id = :id").setParameter("id", id).executeUpdate();

                    // 4. Finally, remove User
                    entityManager.remove(user);
                }
                break;
            case "FACULTY":
                Faculty faculty = entityManager.find(Faculty.class, id);
                if (faculty != null) {
                    entityManager.createNativeQuery("UPDATE discipline_logs SET recorded_by = NULL WHERE recorded_by = :id").setParameter("id", id).executeUpdate();
                    
                    entityManager.createNativeQuery("DELETE FROM attendance_sessions WHERE teacher_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_assignments WHERE faculty_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM attendance WHERE faculty_id = :id").setParameter("id", id).executeUpdate();

                    entityManager.remove(faculty);
                }
                break;
            case "STUDENT":
                Student student = entityManager.find(Student.class, id);
                if (student != null) {
                    entityManager.createNativeQuery("UPDATE stage_teams SET captain_id = NULL WHERE captain_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("UPDATE stage_teams SET vice_captain_id = NULL WHERE vice_captain_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("UPDATE teams SET captain_id = NULL WHERE captain_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("UPDATE teams SET vice_captain_id = NULL WHERE vice_captain_id = :id").setParameter("id", id).executeUpdate();

                    entityManager.createNativeQuery("DELETE FROM student_activity_xp WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM mission_submissions WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM group_members WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    // Fix: reg_no is a BIGINT foreign key mapped to students.id, NOT the string reg_no
                    entityManager.createNativeQuery("DELETE FROM student_points_history WHERE student_id = :id OR reg_no = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM team_removal_requests WHERE student_id = :id OR captain_id = :id OR reg_no = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM attendance_records WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM student_guardians WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM xp_transactions WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM student_activity_streaks WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM team_members WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM streaks WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM attendance WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_completion_requests WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM badge_requests WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM discipline_logs WHERE student_id = :id OR reg_no = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM notifications WHERE student_id = :id OR reg_no = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM penalty_requests WHERE student_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM student_badges WHERE student_id = :id").setParameter("id", id).executeUpdate();

                    entityManager.remove(student);
                }
                break;
            case "ACTIVITY":
                Activity activity = entityManager.find(Activity.class, id);
                if (activity != null) {
                    entityManager.createNativeQuery("DELETE FROM student_points_history WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM stage_activity_mappings WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_assignments WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_stage_mappings WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM student_activity_xp WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM xp_transactions WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM student_activity_streaks WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_temporary_assignments WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_completion_requests WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM discipline_logs WHERE activity_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM penalty_requests WHERE activity_id = :id").setParameter("id", id).executeUpdate();

                    entityManager.remove(activity);
                }
                break;
            case "TEAM":
                Team team = entityManager.find(Team.class, id);
                if (team != null) {
                    entityManager.createNativeQuery("UPDATE students SET team_id = NULL WHERE team_id = :id").setParameter("id", id).executeUpdate();
                    
                    entityManager.createNativeQuery("DELETE FROM team_removal_requests WHERE team_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM stage_teams WHERE team_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM team_members WHERE team_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM activity_completion_requests WHERE team_id = :id").setParameter("id", id).executeUpdate();

                    entityManager.remove(team);
                }
                break;
            case "DEPARTMENT":
                adminDepartmentCommandService.permanentlyDeleteDepartment(id);
                // Return here so it doesn't execute the generic audit log (which is already done in the service)
                return;
            case "BADGE":
                Badge badge = entityManager.find(Badge.class, id);
                if (badge != null) {
                    entityManager.createNativeQuery("DELETE FROM badge_requests WHERE badge_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM student_badges WHERE badge_id = :id").setParameter("id", id).executeUpdate();
                    entityManager.remove(badge);
                }
                break;
            case "ACTIVITY_CATEGORY":
            case "CATEGORY":
                ActivityCategory activityCategory = entityManager.find(ActivityCategory.class, id);
                if (activityCategory != null) {
                    entityManager.createNativeQuery("UPDATE activities SET xp_category = 'General' WHERE xp_category = :name")
                            .setParameter("name", activityCategory.getName())
                            .executeUpdate();
                    entityManager.remove(activityCategory);
                }
                break;
            case "ACTIVITY_EVIDENCE":
            case "EVIDENCE":
                ActivityEvidence activityEvidence = entityManager.find(ActivityEvidence.class, id);
                if (activityEvidence != null) {
                    entityManager.remove(activityEvidence);
                }
                break;
            case "LEVEL":
                Level levelToDelete = entityManager.find(Level.class, id);
                if (levelToDelete != null) {
                    entityManager.remove(levelToDelete);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }

        String moduleStr = entityType.toUpperCase();
        if (moduleStr.equals("FACULTY")) {
            moduleStr = "TEACHER";
        } else if (moduleStr.equals("ACTIVITY_CATEGORY") || moduleStr.equals("CATEGORY")
                || moduleStr.equals("ACTIVITY_EVIDENCE") || moduleStr.equals("EVIDENCE")) {
            moduleStr = "ACTIVITY";
        }

        auditService.log(
            jjcet.PragatiX.enums.AuditAction.PERMANENT_DELETE,
            jjcet.PragatiX.enums.AuditModule.valueOf(moduleStr),
            entityType,
            id,
            "Permanently deleted " + entityType.toLowerCase() + " from Recycle Bin"
        );
    }

    @Transactional
    public int clearAllItems() {
        List<RecycleBinItem> items = getDeletedItems();
        int count = 0;
        for (RecycleBinItem item : items) {
            try {
                permanentlyDeleteItem(item.getEntityType(), item.getId());
                count++;
            } catch (Exception ignored) {
            }
        }
        return count;
    }
}
