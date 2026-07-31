package com.pragatix.modules.admin.mapper;

import com.pragatix.modules.authentication.dto.response.UserResponse;
import com.pragatix.entity.Role;
import com.pragatix.entity.User;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.modules.activity.dto.response.ActivityAssignmentResponse;
import com.pragatix.entity.SubRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Component
public class AdminMapper {
    private static final Logger log = LoggerFactory.getLogger(AdminMapper.class);

    public UserResponse toUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .active(user.isActive())
                .roles(roleNames)
                .subRoles(user.getSubRoles().stream().map(SubRole::getName).collect(Collectors.toSet()))
                .departmentId(deptId)
                .departmentName(deptName)
                .section(user.getSection() != null ? user.getSection().getSectionName() : null)
                .sectionId(user.getSection() != null ? user.getSection().getId() : null)
                .sectionName(user.getSection() != null ? user.getSection().getSectionName() : null)
                .year(user.getYear())
                .build();
    }

    public ActivityAssignmentResponse toActivityAssignmentResponse(ActivityAssignment aa) {
        if (aa == null)
            return null;
        return ActivityAssignmentResponse.builder()
                .id(aa.getId())
                .activityId(aa.getActivity() != null ? aa.getActivity().getId() : null)
                .activityName(aa.getActivity() != null ? aa.getActivity().getName() : null)
                .departmentId(aa.getDepartment() != null ? aa.getDepartment().getId() : null)
                .departmentName(aa.getDepartment() != null ? aa.getDepartment().getName() : null)
                .sectionId(aa.getSection() != null ? aa.getSection().getId() : null)
                .sectionName(aa.getSection() != null ? aa.getSection().getSectionName() : null)
                .teacherId(aa.getTeacher() != null ? aa.getTeacher().getId() : 0L)
                .teacherName(aa.getTeacher() != null ? aa.getTeacher().getFullName() : "Any Faculty")
                .teacherUsername(aa.getTeacher() != null ? aa.getTeacher().getUsername() : "any")
                .assignedBy(aa.getAssignedBy() != null ? aa.getAssignedBy().getFullName() : null)
                .assignedAt(aa.getAssignedAt())
                .year(aa.getYear())
                .assignmentScope(aa.getAssignmentScope() != null ? aa.getAssignmentScope().name() : null)
                .build();
    }

}
