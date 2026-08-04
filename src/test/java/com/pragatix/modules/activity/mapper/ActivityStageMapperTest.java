package com.pragatix.modules.activity.mapper;

import com.pragatix.entity.ActivityStage;
import com.pragatix.enums.StageStatus;
import com.pragatix.modules.activity.dto.request.ActivityStageRequest;
import com.pragatix.modules.activity.dto.response.ActivityStageResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ActivityStageMapperTest {

    private final ActivityStageMapper mapper = new ActivityStageMapper();

    @Test
    void toEntityShouldMapRequestAndUseDefaultsForMissingValues() {
        ActivityStageRequest request = new ActivityStageRequest();
        request.setName("Stage A");
        request.setDescription("desc");
        request.setExpectedXp(null);
        request.setStartDateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        request.setEndDateTime(LocalDateTime.of(2026, 1, 1, 12, 0));
        request.setDisplayOrder(2);
        request.setUseDateValidation(true);
        request.setUseThresholdValidation(true);
        request.setUseCombinedValidation(true);
        request.setMustThreshold(null);
        request.setIndividualThreshold(null);
        request.setGroupThreshold(null);

        ActivityStage entity = mapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("Stage A", entity.getName());
        assertEquals("Stage A", entity.getStageName());
        assertEquals("desc", entity.getDescription());
        assertEquals(0, entity.getExpectedXp());
        assertEquals(request.getStartDateTime(), entity.getStartDateTime());
        assertEquals(request.getEndDateTime(), entity.getEndDateTime());
        assertEquals(2, entity.getDisplayOrder());
        assertTrue(entity.isUseDateValidation());
        assertTrue(entity.isUseThresholdValidation());
        assertTrue(entity.isUseCombinedValidation());
        assertEquals(0, entity.getMustThreshold());
        assertEquals(0, entity.getIndividualThreshold());
        assertEquals(0, entity.getGroupThreshold());
        assertEquals(StageStatus.UPCOMING, entity.getStatus());
    }

    @Test
    void toEntityShouldReturnNullForNullRequest() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityShouldCopyValuesAndIgnoreNullInputs() {
        ActivityStage entity = ActivityStage.builder()
                .name("Old")
                .stageName("Old")
                .description("old")
                .expectedXp(10)
                .startDateTime(LocalDateTime.of(2025, 1, 1, 9, 0))
                .endDateTime(LocalDateTime.of(2025, 1, 1, 11, 0))
                .displayOrder(1)
                .status(StageStatus.COMPLETED)
                .build();

        ActivityStageRequest request = new ActivityStageRequest();
        request.setName("New");
        request.setDescription("new");
        request.setExpectedXp(25);
        request.setStartDateTime(LocalDateTime.of(2026, 2, 2, 8, 0));
        request.setEndDateTime(LocalDateTime.of(2026, 2, 2, 10, 0));
        request.setDisplayOrder(4);
        request.setUseDateValidation(false);
        request.setUseThresholdValidation(true);
        request.setUseCombinedValidation(false);
        request.setMustThreshold(3);
        request.setIndividualThreshold(5);
        request.setGroupThreshold(7);

        mapper.updateEntity(request, entity);

        assertEquals("New", entity.getName());
        assertEquals("New", entity.getStageName());
        assertEquals("new", entity.getDescription());
        assertEquals(25, entity.getExpectedXp());
        assertEquals(request.getStartDateTime(), entity.getStartDateTime());
        assertEquals(request.getEndDateTime(), entity.getEndDateTime());
        assertEquals(4, entity.getDisplayOrder());
        assertFalse(entity.isUseDateValidation());
        assertTrue(entity.isUseThresholdValidation());
        assertFalse(entity.isUseCombinedValidation());
        assertEquals(3, entity.getMustThreshold());
        assertEquals(5, entity.getIndividualThreshold());
        assertEquals(7, entity.getGroupThreshold());
    }

    @Test
    void updateEntityShouldIgnoreNullArguments() {
        ActivityStage entity = ActivityStage.builder().name("Existing").build();

        mapper.updateEntity(null, entity);
        mapper.updateEntity(new ActivityStageRequest(), null);

        assertEquals("Existing", entity.getName());
    }

    @Test
    void toResponseShouldPopulateStateFlagsAndCountdowns() {
        ActivityStage entity = ActivityStage.builder()
                .name("Stage")
                .description("desc")
                .expectedXp(100)
                .startDateTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .endDateTime(LocalDateTime.now().plusDays(1))
                .displayOrder(3)
                .status(StageStatus.UPCOMING)
                .build();

        ActivityStageResponse response = mapper.toResponse(entity);

        assertNotNull(response);
        assertEquals("Stage", response.getName());
        assertEquals("desc", response.getDescription());
        assertEquals(100, response.getExpectedXp());
        assertEquals(3, response.getDisplayOrder());
        assertEquals(StageStatus.UPCOMING, response.getStatus());
        assertTrue(response.getIsUpcoming());
        assertFalse(response.getIsActive());
        assertFalse(response.getIsCompleted());
        assertNotNull(response.getCountdown());
        assertNotNull(response.getRemainingTime());
    }
}
