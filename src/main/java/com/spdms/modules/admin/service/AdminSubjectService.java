package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Subject;
import com.pragatix.repository.SubjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@Service
public class AdminSubjectService {
    private static final Logger log = LoggerFactory.getLogger(AdminSubjectService.class);

    private final SubjectRepository subjectRepository;

    public AdminSubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public ResponseEntity<ApiResponse<List<Subject>>> getAllSubjects() {
        return ResponseEntity.ok(ApiResponse.ok(subjectRepository.findAll()));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Subject>> createSubject(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subject name is required"));
        }
        String cleanName = name.trim();
        if (subjectRepository.existsByName(cleanName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subject already exists"));
        }
        Subject subject = new Subject(cleanName);
        Subject saved = subjectRepository.save(subject);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Subject created successfully", saved));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        if (!subjectRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Subject not found"));
        }
        subjectRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Subject deleted successfully", null));
    }

}
