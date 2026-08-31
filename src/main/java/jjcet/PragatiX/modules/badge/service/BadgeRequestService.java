package jjcet.PragatiX.modules.badge.service;

import jjcet.PragatiX.dto.BadgeRequestCreateDto;
import jjcet.PragatiX.dto.BadgeRequestDto;
import jjcet.PragatiX.dto.BadgeRequestStatusUpdateDto;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.student.repository.StudentBadgeRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BadgeRequestService {

    private final BadgeRequestRepository badgeRequestRepository;
    private final StudentBadgeRepository studentBadgeRepository;
    private final StudentRepository studentRepository;
    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;

    public BadgeRequestService(
            BadgeRequestRepository badgeRequestRepository,
            StudentBadgeRepository studentBadgeRepository,
            StudentRepository studentRepository,
            BadgeRepository badgeRepository,
            UserRepository userRepository,
            AuthUtils authUtils) {
        this.badgeRequestRepository = badgeRequestRepository;
        this.studentBadgeRepository = studentBadgeRepository;
        this.studentRepository = studentRepository;
        this.badgeRepository = badgeRepository;
        this.userRepository = userRepository;
        this.authUtils = authUtils;
    }

    @Transactional
    public BadgeRequestDto createRequest(BadgeRequestCreateDto dto, String username) {
        Student student = studentRepository.findByRegNo(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Badge badge = badgeRepository.findByIdAndDeletedFalse(dto.getBadgeId())
                .orElseThrow(() -> new RuntimeException("Badge not found"));

        if (badge.isProofRequired()) {
            if (dto.getProofLink() == null || dto.getProofLink().trim().isEmpty()) {
                throw new IllegalArgumentException("Proof link is required for this badge");
            }
        }

        List<StudentBadge> earnedBadges = studentBadgeRepository.findByStudentIdAndBadgeId(student.getId(),
                badge.getId());
        if (earnedBadges.stream().anyMatch(b -> "APPROVED".equalsIgnoreCase(b.getStatus()))) {
            throw new IllegalArgumentException("Student already has this badge");
        }
        if (earnedBadges.stream().anyMatch(b -> "PENDING".equalsIgnoreCase(b.getStatus()))) {
            throw new IllegalArgumentException("A pending request already exists for this badge");
        }

        List<BadgeRequest> existing = badgeRequestRepository.findByStudentIdAndBadgeId(student.getId(), badge.getId());
        if (existing.stream().anyMatch(r -> "PENDING".equals(r.getStatus()))) {
            throw new IllegalArgumentException("A pending request already exists for this badge");
        }

        String proofLink = dto.getProofLink() != null && !dto.getProofLink().trim().isEmpty()
                ? dto.getProofLink().trim()
                : null;

        BadgeRequest request = new BadgeRequest();
        request.setStudent(student);
        request.setBadge(badge);
        request.setDepartment(student.getDepartment());
        request.setSection(student.getSection());
        request.setProofLink(proofLink);
        request.setStatus("PENDING");
        request.setRequestedAt(LocalDateTime.now());

        badgeRequestRepository.save(request);

        return toDto(request);
    }

    @Transactional(readOnly = true)
    public List<BadgeRequestDto> getMyRequests(String username) {
        Student student = studentRepository.findByRegNo(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return badgeRequestRepository.findByStudentId(student.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BadgeRequestDto> getAllRequests() {
        User currentUser = authUtils.getCurrentUser();
        List<BadgeRequest> requests = badgeRequestRepository.findAll();

        if (currentUser != null && authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
            String adminYear = AuthUtils.getAssignedYearString(currentUser.getAcademicYear());
            if (adminYear != null) {
                requests = requests.stream()
                        .filter(r -> r.getStudent() != null && adminYear.equals(r.getStudent().getYear()))
                        .collect(Collectors.toList());
            } else if (currentUser.getDepartment() != null) {
                requests = requests.stream()
                        .filter(r -> r.getStudent() != null && r.getStudent().getDepartment() != null
                                && currentUser.getDepartment().getId().equals(r.getStudent().getDepartment().getId()))
                        .collect(Collectors.toList());
            }
        }

        return requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BadgeRequestDto> getCCRequests(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("CC not found"));
        List<BadgeRequest> all = badgeRequestRepository.findAll();

        return all.stream().filter(r -> {
            if (r.getStudent() == null) return false;
            Student s = r.getStudent();

            // Department check: if CC has department, student must match
            if (user.getDepartment() != null && s.getDepartment() != null) {
                if (!user.getDepartment().getId().equals(s.getDepartment().getId())) {
                    return false;
                }
            }

            // Year check: if CC has an assigned year, student must match
            String ccYear = AuthUtils.getAssignedYearString(user.getAcademicYear());
            if (ccYear == null && user.getYear() != null) {
                ccYear = user.getYear();
            }
            if (ccYear != null && s.getYear() != null) {
                if (!ccYear.trim().equalsIgnoreCase(s.getYear().trim())) {
                    return false;
                }
            }

            // Section check: if CC has section AND student has section, they must match
            if (user.getSection() != null && s.getSection() != null) {
                if (!user.getSection().getId().equals(s.getSection().getId())) {
                    return false;
                }
            }

            return true;
        }).map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public BadgeRequestDto approveRequest(Long id, String username) {
        BadgeRequest request = badgeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request is not pending");
        }

        request.setStatus("APPROVED");
        request.setReviewedBy(username);
        request.setReviewedAt(LocalDateTime.now());
        badgeRequestRepository.save(request);

        List<StudentBadge> existing = studentBadgeRepository.findByStudentIdAndBadgeId(
                request.getStudent().getId(), request.getBadge().getId());

        StudentBadge sb;
        if (!existing.isEmpty()) {
            sb = existing.get(0);
        } else {
            sb = new StudentBadge();
            sb.setStudent(request.getStudent());
            sb.setBadge(request.getBadge());
        }
        sb.setStatus("APPROVED");
        sb.setEvidenceUrl(request.getProofLink());
        sb.setAwardedAt(LocalDateTime.now());
        sb.setApprovedBy(username);
        studentBadgeRepository.save(sb);

        return toDto(request);
    }

    @Transactional
    public BadgeRequestDto rejectRequest(Long id, BadgeRequestStatusUpdateDto dto, String username) {
        BadgeRequest request = badgeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request is not pending");
        }

        request.setStatus("REJECTED");
        request.setReviewedBy(username);
        request.setReviewedAt(LocalDateTime.now());
        if (dto != null && dto.getRemarks() != null) {
            request.setRemarks(dto.getRemarks());
        }
        badgeRequestRepository.save(request);

        List<StudentBadge> existing = studentBadgeRepository.findByStudentIdAndBadgeId(
                request.getStudent().getId(), request.getBadge().getId());
        for (StudentBadge sb : existing) {
            if ("PENDING".equalsIgnoreCase(sb.getStatus())) {
                sb.setStatus("REJECTED");
                studentBadgeRepository.save(sb);
            }
        }

        return toDto(request);
    }

    private BadgeRequestDto toDto(BadgeRequest r) {
        try {
            BadgeRequestDto dto = new BadgeRequestDto();
            dto.setId(r.getId());
            dto.setStudentId(r.getStudent().getId());
            dto.setStudentName(r.getStudent().getFullName());
            dto.setRegNo(r.getStudent().getRegNo());
            dto.setBadgeId(r.getBadge().getId());
            dto.setBadgeName(r.getBadge().getName());
            dto.setBadgeIcon(r.getBadge().getIconUrl());
            try {
                dto.setDepartmentName(r.getDepartment() != null ? r.getDepartment().getName() : "");
                dto.setDepartmentId(r.getDepartment() != null ? r.getDepartment().getId() : null);
            } catch (jakarta.persistence.EntityNotFoundException ex) {
                dto.setDepartmentName("");
                dto.setDepartmentId(null);
            }

            try {
                dto.setSectionName(r.getSection() != null ? r.getSection().getSectionName() : "");
                dto.setSectionId(r.getSection() != null ? r.getSection().getId() : null);
            } catch (jakarta.persistence.EntityNotFoundException ex) {
                dto.setSectionName("");
                dto.setSectionId(null);
            }

            try {
                dto.setAcademicYear(r.getStudent() != null && r.getStudent().getYearRef() != null ? r.getStudent().getYearRef().getYearName() : "");
            } catch (jakarta.persistence.EntityNotFoundException ex) {
                dto.setAcademicYear("");
            }
            dto.setStatus(r.getStatus());
            dto.setRequestedAt(r.getRequestedAt());
            dto.setReviewedAt(r.getReviewedAt());
            dto.setReviewedBy(r.getReviewedBy());
            dto.setRemarks(r.getRemarks());
            dto.setProofLink(r.getProofLink());
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error mapping BadgeRequest to DTO: " + e.getMessage(), e);
        }
    }
}
