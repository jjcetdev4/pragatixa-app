package jjcet.PragatiX.modules.activity.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import jjcet.PragatiX.dto.TeamResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.activity.repository.*;
import jjcet.PragatiX.modules.student.repository.*;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.service.XpEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/group-activities")
@Tag(name = "Group Activities", description = "Management of group activities by Faculty")
@SecurityRequirement(name = "bearerAuth")
public class GroupActivityController {

    private static final Logger log = LoggerFactory.getLogger(GroupActivityController.class);

    private final TeamRepository teamRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final XpEngineService xpEngineService;
    private final StageTeamRepository stageTeamRepository;
    private final ActivityStageRepository activityStageRepository;

    public GroupActivityController(TeamRepository teamRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            XpEngineService xpEngineService,
            StageTeamRepository stageTeamRepository,
            ActivityStageRepository activityStageRepository) {
        this.teamRepository = teamRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.xpEngineService = xpEngineService;
        this.stageTeamRepository = stageTeamRepository;
        this.activityStageRepository = activityStageRepository;
    }

    @GetMapping("/assignments/{assignmentId}/teams")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Get Teams for Assignment", description = "Returns all teams created for a specific activity assignment.")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsForAssignment(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Long stageId) {
        ActivityAssignment assignment = activityAssignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Assignment not found"));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        boolean canDelete = false;
        if (currentUser != null) {
            boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("ROLE_ADMIN"));
            boolean isCc = currentUser.getSubRoles().stream().map(jjcet.PragatiX.entity.SubRole::getName)
                    .anyMatch(sr -> sr.trim().equalsIgnoreCase("CC"));
            boolean isAssignedFaculty = assignment.getTeacher() != null
                    && assignment.getTeacher().getUsername().equals(username);

            boolean matchesDeptAndSection = false;
            if (isCc && assignment.getDepartment() != null && currentUser.getDepartment() != null) {
                if (assignment.getDepartment().getId().equals(currentUser.getDepartment().getId())) {
                    if (assignment.getSection() == null || (currentUser.getSection() != null
                            && assignment.getSection().getId().equals(currentUser.getSection().getId()))) {
                        matchesDeptAndSection = true;
                    }
                }
            }
            canDelete = isAdmin || isAssignedFaculty || matchesDeptAndSection;
        }
        final boolean finalCanDelete = canDelete;

        Activity activity = assignment.getActivity();
        int stageOrder = 0;
        Long targetStageId = null;

        if (stageId != null && stageId > 0) {
            ActivityStage explicitStage = activityStageRepository.findById(stageId).orElse(null);
            if (explicitStage != null) {
                stageOrder = explicitStage.getDisplayOrder();
                targetStageId = explicitStage.getId();
            }
        }

        if (stageOrder <= 0) {
            if (assignment.getStage() != null) {
                stageOrder = assignment.getStage().getDisplayOrder();
                targetStageId = assignment.getStage().getId();
            } else if (activity != null && activity.getStage() != null) {
                stageOrder = activity.getStage().getDisplayOrder();
                targetStageId = activity.getStage().getId();
            } else if (activity != null && activity.getSubgroup() != null
                    && activity.getSubgroup().getStage() != null) {
                stageOrder = activity.getSubgroup().getStage().getDisplayOrder();
                targetStageId = activity.getSubgroup().getStage().getId();
            }
        }
        final int activityStageOrder = stageOrder;
        final Long finalTargetStageId = targetStageId;

        log.debug("[GroupActivity] assignmentId={} stageId={} targetStageOrder={} scope={} dept={} year={} section={}",
                assignmentId,
                finalTargetStageId,
                activityStageOrder,
                assignment.getAssignmentScope(),
                assignment.getDepartment() != null ? assignment.getDepartment().getId() : "null",
                assignment.getYear(),
                assignment.getSection() != null ? assignment.getSection().getId() : "null");

        List<Team> teams = teamRepository.findAll().stream().filter(t -> {
            if (assignment.getAssignmentScope() == AssignmentScope.GLOBAL) {
                // global scope still respects stage eligibility
            } else {
                if (t.getDepartment() == null || assignment.getDepartment() == null)
                    return false;
                if (!t.getDepartment().getId().equals(assignment.getDepartment().getId()))
                    return false;
                if (assignment.getAssignmentScope() != AssignmentScope.DEPARTMENT) {
                    // Use fuzzy year matching — teams may store "I" while assignment stores "1",
                    // etc.
                    int teamYearNo = normalizeYearToInt(t.getYear());
                    int assignYearNo = normalizeYearToInt(assignment.getYear());
                    // If assignment has no year set, skip year filter (include all years for
                    // dept/section)
                    if (assignYearNo != -1 && (teamYearNo == -1 || teamYearNo != assignYearNo))
                        return false;
                    if (assignment.getSection() != null) {
                        if (t.getSection() == null || !t.getSection().getId().equals(assignment.getSection().getId())) {
                            return false;
                        }
                    }
                }
            }

            // Stage Eligibility Filtering for Team
            if (activityStageOrder > 0 || finalTargetStageId != null) {
                List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(t.getId());
                if (stageTeams != null && !stageTeams.isEmpty()) {
                    boolean stageMatches = stageTeams.stream().anyMatch(st -> {
                        if (st.getStage() == null)
                            return false;
                        if (finalTargetStageId != null && st.getStage().getId().equals(finalTargetStageId)) {
                            return true;
                        }
                        return activityStageOrder > 0 && st.getStage().getDisplayOrder() == activityStageOrder;
                    });
                    if (!stageMatches) {
                        return false;
                    }
                } else {
                    // No StageTeam link found in DB -> evaluate team stage from captain or members
                    int teamCurrentStage = 1; // Default to Stage 1
                    if (t.getCaptain() != null) {
                        int capStage = t.getCaptain().getCurrentStage() > 0 ? t.getCaptain().getCurrentStage()
                                : t.getCaptain().getStage();
                        teamCurrentStage = capStage > 0 ? capStage : 1;
                    } else if (t.getMembers() != null && !t.getMembers().isEmpty()) {
                        int memStage = t.getMembers().stream()
                                .filter(Student::isActive)
                                .map(s -> s.getCurrentStage() > 0 ? s.getCurrentStage() : s.getStage())
                                .findFirst()
                                .orElse(1);
                        teamCurrentStage = memStage > 0 ? memStage : 1;
                    }

                    if (activityStageOrder > 0 && teamCurrentStage != activityStageOrder) {
                        return false;
                    }
                }
            }

            return true;
        }).collect(Collectors.toList());
        log.debug("[GroupActivity] teams found: {}", teams.size());

        List<TeamResponse> responses = teams.stream().map(g -> {
            List<StudentResponse> studentResponses = g.getMembers().stream()
                    .filter(Student::isActive)
                    .filter(s -> {
                        if (activityStageOrder <= 0)
                            return true;
                        int sStage = s.getCurrentStage() > 0 ? s.getCurrentStage()
                                : (s.getStage() > 0 ? s.getStage() : 1);
                        return sStage == activityStageOrder;
                    })
                    .map(s -> toStudentResponse(g, s))
                    .collect(Collectors.toList());

            String captainId = null;
            String captainName = null;

            if (g.getCaptain() != null && g.getCaptain().isActive()) {
                int capStage = g.getCaptain().getCurrentStage() > 0 ? g.getCaptain().getCurrentStage()
                        : (g.getCaptain().getStage() > 0 ? g.getCaptain().getStage() : 1);
                if (activityStageOrder <= 0 || capStage == activityStageOrder) {
                    final String finalCaptainId = g.getCaptain().getRegNo();
                    captainId = finalCaptainId;
                    captainName = g.getCaptain().getFullName();
                    boolean captainInMembers = studentResponses.stream()
                            .anyMatch(s -> s.getRegNo().equals(finalCaptainId));
                    if (!captainInMembers) {
                        studentResponses.add(0, toStudentResponse(g, g.getCaptain()));
                    }
                }
            }

            String viceCaptainId = null;
            String viceCaptainName = null;

            Student resolvedVc = g.getViceCaptain();
            if (resolvedVc == null) {
                List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(g.getId());
                if (stageTeams != null) {
                    for (StageTeam st : stageTeams) {
                        if (st.getViceCaptain() != null) {
                            resolvedVc = st.getViceCaptain();
                            break;
                        }
                    }
                }
            }

            if (resolvedVc != null && resolvedVc.isActive()) {
                int vcStage = resolvedVc.getCurrentStage() > 0 ? resolvedVc.getCurrentStage()
                        : (resolvedVc.getStage() > 0 ? resolvedVc.getStage() : 1);
                if (activityStageOrder <= 0 || vcStage == activityStageOrder) {
                    final String finalVcId = resolvedVc.getRegNo();
                    viceCaptainId = finalVcId;
                    viceCaptainName = resolvedVc.getFullName();
                    boolean vcInMembers = studentResponses.stream()
                            .anyMatch(s -> s.getRegNo().equals(finalVcId));
                    if (!vcInMembers) {
                        studentResponses.add(toStudentResponse(g, resolvedVc));
                    }
                }
            }

            studentResponses.sort((s1, s2) -> {
                int xp1 = s1.getTotalXp() > 0 ? s1.getTotalXp() : s1.getCurrentXp();
                int xp2 = s2.getTotalXp() > 0 ? s2.getTotalXp() : s2.getCurrentXp();
                if (xp1 != xp2) {
                    return Integer.compare(xp2, xp1);
                }
                boolean isCap1 = "CAPTAIN".equalsIgnoreCase(s1.getTeamRole());
                boolean isCap2 = "CAPTAIN".equalsIgnoreCase(s2.getTeamRole());
                if (isCap1 && !isCap2) return -1;
                if (!isCap1 && isCap2) return 1;

                boolean isVc1 = "VICE_CAPTAIN".equalsIgnoreCase(s1.getTeamRole());
                boolean isVc2 = "VICE_CAPTAIN".equalsIgnoreCase(s2.getTeamRole());
                if (isVc1 && !isVc2) return -1;
                if (!isVc1 && isVc2) return 1;

                String n1 = s1.getFullName() != null ? s1.getFullName() : "";
                String n2 = s2.getFullName() != null ? s2.getFullName() : "";
                return n1.compareToIgnoreCase(n2);
            });

            return new TeamResponse(
                    g.getId(),
                    g.getName(),
                    g.getSize(),
                    captainId,
                    captainName,
                    viceCaptainId,
                    viceCaptainName,
                    studentResponses,
                    assignment.getId(),
                    assignment.getActivity() != null ? assignment.getActivity().getActivityName() : "",
                    finalCanDelete);
        })
                .sorted(Comparator.comparing(
                        (TeamResponse tr) -> tr.getTeamName() != null ? tr.getTeamName().trim() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping("/teams/{teamId}/award-xp")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional
    @Operation(summary = "Award XP to Team", description = "Awards XP to all or selected members of a team with remarks.")
    public ResponseEntity<ApiResponse<String>> awardXpToTeam(@PathVariable Long teamId,
            @RequestBody Map<String, Object> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User teacher = userRepository.findByUsername(username).orElse(null);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Teacher not found"));
        }

        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Team not found"));
        }

        if (!body.containsKey("assignmentId")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("assignmentId must be provided in the request body"));
        }
        Long assignmentId = Long.valueOf(body.get("assignmentId").toString());
        ActivityAssignment assignment = activityAssignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Assignment not found"));
        }

        Activity activity = assignment.getActivity();
        if (activity.getStage() != null && activity.getStage().getStatus() != jjcet.PragatiX.enums.StageStatus.ACTIVE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot award XP for an activity in a non-active stage."));
        }

        int stageOrder = 0;
        if (activity.getStage() != null) {
            stageOrder = activity.getStage().getDisplayOrder();
        } else if (assignment.getStage() != null) {
            stageOrder = assignment.getStage().getDisplayOrder();
        } else if (activity.getSubgroup() != null && activity.getSubgroup().getStage() != null) {
            stageOrder = activity.getSubgroup().getStage().getDisplayOrder();
        }
        final int activityStageOrder = stageOrder;

        boolean equalDistribution = body.containsKey("equalDistribution")
                && Boolean.parseBoolean(body.get("equalDistribution").toString());

        if (equalDistribution) {
            int xp = 0;
            if (body.containsKey("isPenalty") && Boolean.parseBoolean(body.get("isPenalty").toString())) {
                // If it's a penalty, make sure xp is negative
                int absXp = Math.abs(Integer.parseInt(body.get("xp").toString()));
                xp = -absXp;
            } else {
                xp = Integer.parseInt(body.get("xp").toString());
            }
            
            String remarks = body.containsKey("remarks") ? body.get("remarks").toString() : null;

            System.out.println("GROUP XP DEBUG: Processing team " + team.getId() + " with " + team.getMembers().size() + " members. XP: " + xp);

            List<Student> studentsToAward = new ArrayList<>(team.getMembers());
            if (team.getCaptain() != null && !studentsToAward.contains(team.getCaptain())) {
                studentsToAward.add(team.getCaptain());
            }

            for (Student member : studentsToAward) {
                if (member.isActive()) {
                    System.out.println("GROUP XP DEBUG: Member " + member.getRegNo() + " qualifies! Awarding...");
                    xpEngineService.awardXp(member, activity, teacher, assignment, xp, remarks);
                } else {
                    System.out.println("GROUP XP DEBUG: Member " + member.getRegNo() + " rejected! Active? " + member.isActive());
                }
            }
        } else {
            List<Map<String, Object>> studentsData = (List<Map<String, Object>>) body.get("students");
            if (studentsData == null || studentsData.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No student data provided"));
            }

            List<String> studentIds = studentsData.stream().map(s -> s.get("regNo").toString())
                    .collect(Collectors.toList());
            List<Student> fetchedStudents = studentRepository.findByRegNoIn(studentIds);
            Map<String, Student> studentMap = fetchedStudents.stream()
                    .collect(Collectors.toMap(Student::getRegNo, s -> s));

            for (Map<String, Object> sData : studentsData) {
                String regNo = sData.get("regNo").toString();
                int xp = 0;
                if (sData.containsKey("isPenalty") && Boolean.parseBoolean(sData.get("isPenalty").toString())) {
                    int absXp = Math.abs(Integer.parseInt(sData.get("xp").toString()));
                    xp = -absXp;
                } else {
                    xp = Integer.parseInt(sData.get("xp").toString());
                }
                
                String remarks = sData.containsKey("remarks") ? sData.get("remarks").toString() : null;

                Student student = studentMap.get(regNo);
                if (student != null && student.isActive()) {
                    System.out.println("GROUP XP DEBUG: Member " + student.getRegNo() + " qualifies! Awarding...");
                    xpEngineService.awardXp(student, activity, teacher, assignment, xp, remarks);
                } else if (student != null) {
                    System.out.println("GROUP XP DEBUG: Member " + student.getRegNo() + " rejected! Active? " + student.isActive());
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("XP awarded successfully", null));
    }

    private String resolveTeamRole(Team team, Student student) {
        if (team == null || student == null)
            return "MEMBER";

        if (team.getCaptain() != null && team.getCaptain().getId().equals(student.getId())) {
            return "CAPTAIN";
        }

        if (team.getViceCaptain() != null && team.getViceCaptain().getId().equals(student.getId())) {
            return "VICE_CAPTAIN";
        }

        List<StageTeam> stageTeams = stageTeamRepository.findByTeamId(team.getId());
        if (stageTeams != null) {
            for (StageTeam st : stageTeams) {
                if (st.getViceCaptain() != null && st.getViceCaptain().getId().equals(student.getId())) {
                    return "VICE_CAPTAIN";
                }
            }
        }

        return "MEMBER";
    }

    private StudentResponse toStudentResponse(Team team, Student student) {
        int sStage = student.getCurrentStage() > 0 ? student.getCurrentStage()
                : (student.getStage() > 0 ? student.getStage() : 1);
        StudentResponse s = new StudentResponse();
        s.setId(student.getId());
        s.setRegNo(student.getRegNo());
        s.setFullName(student.getFullName());
        s.setEmail(student.getEmail());
        s.setPhone(student.getPhone());
        s.setDepartmentName(student.getDepartment() != null ? student.getDepartment().getName() : null);
        s.setSection(student.getSection() != null ? student.getSection().getSectionName() : null);
        s.setScore(student.getScore());
        s.setTotalXp(student.getTotalXp());
        s.setCurrentXp(student.getTotalXp());
        s.setMustXp(student.getMustXp());
        s.setIndividualXp(student.getIndividualXp());
        s.setGroupXp(student.getGroupXp());
        s.setCurrentStage(sStage);
        s.setTeamRole(resolveTeamRole(team, student));
        return s;
    }

    /**
     * Normalises year strings like "1", "I", "First Year", "1st year", "1st Year"
     * to an integer (1-4). Returns -1 if the year cannot be determined.
     */
    private int normalizeYearToInt(String year) {
        if (year == null || year.trim().isEmpty())
            return -1;
        String y = year.trim().toUpperCase();
        if (y.equals("1") || y.equals("I") || y.contains("FIRST") || y.startsWith("1ST"))
            return 1;
        if (y.equals("2") || y.equals("II") || y.contains("SECOND") || y.startsWith("2ND"))
            return 2;
        if (y.equals("3") || y.equals("III") || y.contains("THIRD") || y.startsWith("3RD"))
            return 3;
        if (y.equals("4") || y.equals("IV") || y.contains("FOURTH") || y.startsWith("4TH"))
            return 4;
        return -1;
    }
}
