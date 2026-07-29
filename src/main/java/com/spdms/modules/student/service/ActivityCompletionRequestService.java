package com.pragatix.modules.student.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.*;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.student.dto.request.CreateActivityCompletionRequestDto;
import com.pragatix.modules.student.dto.response.ActivityCompletionRequestDto;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.repository.TeamRepository;
import com.pragatix.repository.ActivityCompletionRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityCompletionRequestService {

    private final ActivityCompletionRequestRepository repository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final TeamRepository teamRepository;
    private final StudentAssignmentResolver studentAssignmentResolver;

    public ActivityCompletionRequestService(ActivityCompletionRequestRepository repository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            ActivityRepository activityRepository,
            TeamRepository teamRepository,
            StudentAssignmentResolver studentAssignmentResolver) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
        this.studentAssignmentResolver = studentAssignmentResolver;
    }

    private User findCcForStudent(Student student) {
        List<User> users = userRepository.findAll();
        return users.stream()
                .filter(u -> u.getSubRoles().stream().anyMatch(sr -> "CC".equalsIgnoreCase(sr.getName())))
                .filter(u -> u.getSection() != null && student.getSection() != null
                        && u.getSection().getId().equals(student.getSection().getId()))
                .filter(u -> u.getDepartment() != null && student.getDepartment() != null
                        && u.getDepartment().getId().equals(student.getDepartment().getId()))
                .filter(User::isActive)
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public ApiResponse<ActivityCompletionRequestDto> submitRequest(CreateActivityCompletionRequestDto dto,
            String username) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(username);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        Optional<Activity> activityOpt = activityRepository.findById(dto.getActivityId());
        if (activityOpt.isEmpty()) {
            return ApiResponse.error("Activity not found");
        }
        Activity activity = activityOpt.get();

        if (activity.getAllowStudentRequest() == null || !activity.getAllowStudentRequest()) {
            return ApiResponse.error("This activity does not allow student requests");
        }

        Team team = null;
        if (dto.getTeamId() != null) {
            Optional<Team> teamOpt = teamRepository.findById(dto.getTeamId());
            if (teamOpt.isEmpty()) {
                return ApiResponse.error("Team not found");
            }
            team = teamOpt.get();
            // Validate Captain
            if (team.getCaptain() == null || !student.getId().equals(team.getCaptain().getId())) {
                return ApiResponse.error("Only the Captain can request completion for a team activity");
            }

            // Check duplicates for team
            List<ActivityCompletionRequest> existing = repository
                    .findByTeamIdAndActivityIdOrderByCreatedAtDesc(team.getId(), activity.getId());
            if (existing.stream().anyMatch(r -> "PENDING".equals(r.getStatus()))) {
                return ApiResponse.error("A request is already pending for this team");
            }
            if (existing.stream().anyMatch(r -> "APPROVED".equals(r.getStatus()))) {
                return ApiResponse.error("This team has already completed this activity");
            }
        } else {
            // Check duplicates for student
            List<ActivityCompletionRequest> existing = repository
                    .findByStudentIdAndActivityIdOrderByCreatedAtDesc(student.getId(), activity.getId());
            if (existing.stream().anyMatch(r -> "PENDING".equals(r.getStatus()))) {
                return ApiResponse.error("A request is already pending");
            }
            if (existing.stream().anyMatch(r -> "APPROVED".equals(r.getStatus()))) {
                return ApiResponse.error("You have already completed this activity");
            }
        }

        ActivityCompletionRequest request = new ActivityCompletionRequest();
        request.setStudent(student);
        request.setTeam(team);
        request.setActivity(activity);
        request.setProofUrl(dto.getProofUrl());
        request.setReason(dto.getReason());
        request.setStatus("PENDING");

        User cc = findCcForStudent(student);
        if (cc != null) {
            request.setCc(cc);
        }

        ActivityCompletionRequest saved = repository.save(request);
        return ApiResponse.ok("Request submitted successfully", mapToDto(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ActivityCompletionRequestDto>> getStudentRequests(String username) {
        Optional<Student> studentOpt = studentRepository.findByRegNo(username);
        if (studentOpt.isEmpty()) {
            return ApiResponse.error("Student not found");
        }
        Student student = studentOpt.get();

        List<ActivityCompletionRequest> requests = repository.findMyRequests(student.getId());

        List<ActivityCompletionRequestDto> dtos = requests.stream().map(this::mapToDto).collect(Collectors.toList());
        return ApiResponse.ok("Fetched student requests", dtos);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ActivityCompletionRequestDto>> getInbox(String username, String status) {
        Optional<User> teacherOpt = userRepository.findByUsername(username);
        if (teacherOpt.isEmpty()) {
            return ApiResponse.error("Teacher not found");
        }
        User teacher = teacherOpt.get();

        System.out.println("--- INBOX API CALLED ---");
        System.out.println("Authenticated Username: " + username);
        System.out.println("Authenticated User ID: " + teacher.getId());
        System.out.println("Authenticated Role: " + (teacher.getRoles() != null && !teacher.getRoles().isEmpty()
                ? teacher.getRoles().iterator().next().getName()
                : "None"));
        System.out.println("Authenticated Department: "
                + (teacher.getDepartment() != null ? teacher.getDepartment().getName() : "None"));
        System.out.println("Authenticated Section: "
                + (teacher.getSection() != null ? teacher.getSection().getSectionName() : "None"));

        // 1. Fetch potential requests where teacher might be assigned or is CC
        List<ActivityCompletionRequest> possibleRequests = repository.findPossibleRequestsForTeacher(teacher.getId(),
                status);

        // 2. Extract unique activity IDs
        List<Long> activityIds = possibleRequests.stream().map(r -> r.getActivity().getId()).distinct()
                .collect(Collectors.toList());

        // 3. Fetch assignments using the resolver
        java.util.Map<Long, List<ActivityAssignment>> assignmentsByActivity = studentAssignmentResolver
                .fetchAssignmentsByActivity(activityIds);

        // 4. Filter strictly
        List<ActivityCompletionRequest> filteredRequests = new java.util.ArrayList<>();
        for (ActivityCompletionRequest r : possibleRequests) {
            boolean include = false;
            List<ActivityAssignment> validAssignments = java.util.Collections.emptyList();

            // Check CC fallback first
            if (r.getCc() != null && r.getCc().getId().equals(teacher.getId())) {
                include = true;
            } else {
                // Check assignment strictly
                List<ActivityAssignment> assignments = assignmentsByActivity.getOrDefault(r.getActivity().getId(),
                        java.util.Collections.emptyList());
                validAssignments = studentAssignmentResolver.resolveAllValidAssignments(r.getStudent(), assignments);

                if (!validAssignments.isEmpty()) {
                    include = validAssignments.stream()
                            .anyMatch(a -> a.getTeacher() != null && a.getTeacher().getId().equals(teacher.getId()));
                } else if (r.getActivity().getSubgroup() != null
                        && r.getActivity().getSubgroup().getAssignedFaculty() != null) {
                    include = r.getActivity().getSubgroup().getAssignedFaculty().getId().equals(teacher.getId());
                }
            }

            if (include) {
                filteredRequests.add(r);
            }

            // Add Debug Log per user request
            System.out.println("----- DEBUG LOG -----");
            System.out.println("Logged-in Teacher ID: " + teacher.getId());
            System.out.println("Activity ID: " + r.getActivity().getId());
            System.out.println("Assigned Teacher IDs: " + validAssignments.stream().filter(a -> a.getTeacher() != null)
                    .map(a -> a.getTeacher().getId().toString()).collect(Collectors.joining(",")));
            System.out.println("Request ID: " + r.getId());
            System.out.println("Is Teacher Assigned = " + (include ? "TRUE" : "FALSE"));
            System.out.println("---------------------");
        }

        System.out.println("Rows Returned: " + filteredRequests.size());

        List<ActivityCompletionRequestDto> dtos = filteredRequests.stream().map(this::mapToDto)
                .collect(Collectors.toList());
        return ApiResponse.ok("Fetched inbox", dtos);
    }

    @Transactional
    public ApiResponse<ActivityCompletionRequestDto> approveRequest(Long id, String username) {
        Optional<ActivityCompletionRequest> reqOpt = repository.findById(id);
        if (reqOpt.isEmpty()) {
            return ApiResponse.error("Request not found");
        }
        ActivityCompletionRequest request = reqOpt.get();
        if (!"PENDING".equals(request.getStatus())) {
            return ApiResponse.error("Request is not PENDING");
        }

        Optional<User> teacherOpt = userRepository.findByUsername(username);
        if (teacherOpt.isEmpty()) {
            return ApiResponse.error("Teacher not found");
        }
        User teacher = teacherOpt.get();

        request.setStatus("APPROVED");
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(teacher.getFullName());

        ActivityCompletionRequest saved = repository.save(request);
        return ApiResponse.ok("Request approved", mapToDto(saved));
    }

    @Transactional
    public ApiResponse<ActivityCompletionRequestDto> rejectRequest(Long id, String username, String reason) {
        Optional<ActivityCompletionRequest> reqOpt = repository.findById(id);
        if (reqOpt.isEmpty()) {
            return ApiResponse.error("Request not found");
        }
        ActivityCompletionRequest request = reqOpt.get();
        if (!"PENDING".equals(request.getStatus())) {
            return ApiResponse.error("Request is not PENDING");
        }

        Optional<User> teacherOpt = userRepository.findByUsername(username);
        if (teacherOpt.isEmpty()) {
            return ApiResponse.error("Teacher not found");
        }
        User teacher = teacherOpt.get();

        request.setStatus("REJECTED");
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(teacher.getFullName());
        request.setRejectedReason(reason);

        ActivityCompletionRequest saved = repository.save(request);
        return ApiResponse.ok("Request rejected", mapToDto(saved));
    }

    private ActivityCompletionRequestDto mapToDto(ActivityCompletionRequest r) {
        ActivityCompletionRequestDto dto = new ActivityCompletionRequestDto();
        dto.setId(r.getId());
        if (r.getStudent() != null) {
            dto.setStudentName(r.getStudent().getFullName());
            dto.setRegNo(r.getStudent().getRegNo());
            if (r.getStudent().getDepartment() != null)
                dto.setDepartment(r.getStudent().getDepartment().getName());
            if (r.getStudent().getYear() != null)
                dto.setYear(r.getStudent().getYear());
            if (r.getStudent().getSection() != null)
                dto.setSection(r.getStudent().getSection().getSectionName());
        }
        if (r.getActivity() != null) {
            dto.setActivityId(r.getActivity().getId());
            dto.setActivityName(r.getActivity().getActivityName());
        }
        if (r.getTeam() != null) {
            dto.setTeamId(r.getTeam().getId());
            dto.setTeamName(r.getTeam().getName());
        }
        dto.setProofUrl(r.getProofUrl());
        dto.setReason(r.getReason());
        dto.setStatus(r.getStatus());
        dto.setRequestedDate(r.getCreatedAt());
        dto.setApprovedDate(r.getApprovedAt());
        dto.setApprovedBy(r.getApprovedBy());
        dto.setRejectedReason(r.getRejectedReason());
        return dto;
    }
}
