package com.pragatix.modules.badge.service;

import com.pragatix.dto.BadgeRequestCreateDto;
import com.pragatix.dto.BadgeRequestDto;
import com.pragatix.dto.BadgeRequestStatusUpdateDto;
import com.pragatix.entity.*;
import com.pragatix.repository.*;
import com.pragatix.modules.student.repository.StudentBadgeRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.authentication.repository.UserRepository;
import com.pragatix.modules.authentication.security.AuthUtils;
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
        Badge badge = badgeRepository.findById(dto.getBadgeId())
                .orElseThrow(() -> new RuntimeException("Badge not found"));

        if (studentBadgeRepository.existsByStudentIdAndBadgeId(student.getId(), badge.getId())) {
            throw new RuntimeException("Student already has this badge");
        }

        List<BadgeRequest> existing = badgeRequestRepository.findByStudentIdAndBadgeId(student.getId(), badge.getId());
        if (existing.stream().anyMatch(r -> "PENDING".equals(r.getStatus()))) {
            throw new RuntimeException("A pending request already exists for this badge");
        }

        BadgeRequest request = new BadgeRequest();
        request.setStudent(student);
        request.setBadge(badge);
        request.setDepartment(student.getDepartment());
        request.setSection(student.getSection());
        request.setProofLink(dto.getProofLink());
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
            } else {
                requests = java.util.Collections.emptyList();
            }
        }

        return requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BadgeRequestDto> getCCRequests(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("CC not found"));
        if (user.getDepartment() == null || user.getSection() == null) {
            throw new RuntimeException("CC is not assigned to a valid department and section");
        }
        return badgeRequestRepository
                .findByDepartmentIdAndSectionId(user.getDepartment().getId(), user.getSection().getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
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

        if (!studentBadgeRepository.existsByStudentIdAndBadgeId(request.getStudent().getId(),
                request.getBadge().getId())) {
            StudentBadge sb = new StudentBadge();
            sb.setStudent(request.getStudent());
            sb.setBadge(request.getBadge());
            sb.setAwardedAt(LocalDateTime.now());
            sb.setStatus("APPROVED");
            sb.setApprovedBy(username);
            studentBadgeRepository.save(sb);
        }

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
            dto.setDepartmentName(r.getDepartment() != null ? r.getDepartment().getName() : "");
            dto.setSectionName(r.getSection() != null ? r.getSection().getSectionName() : "");
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
