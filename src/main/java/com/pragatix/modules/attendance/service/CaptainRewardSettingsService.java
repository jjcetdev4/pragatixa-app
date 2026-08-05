package com.pragatix.modules.attendance.service;

import com.pragatix.entity.CaptainRewardSettings;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.attendance.dto.CaptainRewardSettingsDTO;
import com.pragatix.modules.attendance.repository.CaptainRewardSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CaptainRewardSettingsService {

    @Autowired
    private CaptainRewardSettingsRepository repository;

    public CaptainRewardSettingsDTO getSettings(AcademicYear year) {
        Optional<CaptainRewardSettings> opt = repository.findByAcademicYear(year);
        CaptainRewardSettings entity = opt.orElseGet(() -> {
            CaptainRewardSettings newEntity = new CaptainRewardSettings();
            newEntity.setAcademicYear(year);
            newEntity.setEngineEnabled(false);
            newEntity.setCaptainXp(0);
            newEntity.setViceCaptainXp(0);
            return repository.save(newEntity);
        });

        CaptainRewardSettingsDTO dto = new CaptainRewardSettingsDTO();
        dto.setAcademicYear(entity.getAcademicYear());
        dto.setEngineEnabled(entity.getEngineEnabled());
        dto.setCaptainXp(entity.getCaptainXp());
        dto.setViceCaptainXp(entity.getViceCaptainXp());
        dto.setExecutionTime(entity.getExecutionTime());
        dto.setLastExecutionDate(entity.getLastExecutionDate());
        return dto;
    }

    public CaptainRewardSettingsDTO updateSettings(AcademicYear year, CaptainRewardSettingsDTO request) {
        Optional<CaptainRewardSettings> opt = repository.findByAcademicYear(year);
        CaptainRewardSettings entity = opt.orElseGet(() -> {
            CaptainRewardSettings newEntity = new CaptainRewardSettings();
            newEntity.setAcademicYear(year);
            return newEntity;
        });

        entity.setEngineEnabled(request.getEngineEnabled());
        entity.setCaptainXp(request.getCaptainXp());
        entity.setViceCaptainXp(request.getViceCaptainXp());
        entity.setExecutionTime(request.getExecutionTime());
        
        entity = repository.save(entity);

        CaptainRewardSettingsDTO dto = new CaptainRewardSettingsDTO();
        dto.setAcademicYear(entity.getAcademicYear());
        dto.setEngineEnabled(entity.getEngineEnabled());
        dto.setCaptainXp(entity.getCaptainXp());
        dto.setViceCaptainXp(entity.getViceCaptainXp());
        dto.setExecutionTime(entity.getExecutionTime());
        dto.setLastExecutionDate(entity.getLastExecutionDate());
        return dto;
    }
}
