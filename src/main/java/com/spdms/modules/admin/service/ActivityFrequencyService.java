package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.CustomFrequency;
import com.pragatix.repository.CustomFrequencyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class ActivityFrequencyService {

    private final CustomFrequencyRepository customFrequencyRepository;

    public ActivityFrequencyService(CustomFrequencyRepository customFrequencyRepository) {
        this.customFrequencyRepository = customFrequencyRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CustomFrequency>>> getCustomFrequencies() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched custom frequencies", customFrequencyRepository.findAll()));
    }

    @Transactional
    public ResponseEntity<ApiResponse<CustomFrequency>> createCustomFrequency(Map<String, Object> payload) {
        String name = (String) payload.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.<CustomFrequency>error("Name is required"));
        }

        if (customFrequencyRepository.findByNameIgnoreCase(name).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<CustomFrequency>error("Custom frequency with this name already exists"));
        }

        String capType = (String) payload.getOrDefault("capType", "UNLIMITED");
        Integer defaultCap = payload.containsKey("defaultCap") && payload.get("defaultCap") != null
                ? Integer.parseInt(payload.get("defaultCap").toString())
                : 0;

        CustomFrequency freq = new CustomFrequency(name, capType, defaultCap);
        freq = customFrequencyRepository.save(freq);

        return ResponseEntity.ok(ApiResponse.ok("Custom frequency created", freq));
    }
}
