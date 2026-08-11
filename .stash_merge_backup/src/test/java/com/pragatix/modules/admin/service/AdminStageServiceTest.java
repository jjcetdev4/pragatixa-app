package com.pragatix.modules.admin.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.activity.dto.request.EvaluatePromotionsRequest;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.modules.activity.service.ActivityStageService;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.student.service.XpEngineService;
import com.pragatix.repository.AcademicYearRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminStageServiceTest {

    @Mock
    private ActivityStageService activityStageService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private XpEngineService xpEngineService;

    @Mock
    private ActivityStageRepository activityStageRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    private AdminStageService adminStageService;

    @BeforeEach
    public void setUp() {
        adminStageService = new AdminStageService(
                activityStageService,
                studentRepository,
                xpEngineService,
                activityStageRepository,
                academicYearRepository
        );
    }

    @Test
    public void testEvaluatePromotions_InvalidStageId_NegativeOne() {
        EvaluatePromotionsRequest req = new EvaluatePromotionsRequest(-1L, 1L);
        ResponseEntity<ApiResponse<Void>> response = adminStageService.evaluatePromotions(req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid stage ID", response.getBody().getMessage());
    }

    @Test
    public void testEvaluatePromotions_InvalidAcademicYearId_NegativeOne() {
        when(activityStageRepository.existsById(1L)).thenReturn(true);
        EvaluatePromotionsRequest req = new EvaluatePromotionsRequest(1L, -1L);
        ResponseEntity<ApiResponse<Void>> response = adminStageService.evaluatePromotions(req);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid academic year ID", response.getBody().getMessage());
    }

    @Test
    public void testEvaluatePromotions_StageNotFound() {
        when(activityStageRepository.existsById(999L)).thenReturn(false);
        EvaluatePromotionsRequest req = new EvaluatePromotionsRequest(999L, 1L);
        ResponseEntity<ApiResponse<Void>> response = adminStageService.evaluatePromotions(req);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Stage not found", response.getBody().getMessage());
    }

    @Test
    public void testEvaluatePromotions_AcademicYearNotFound() {
        when(activityStageRepository.existsById(1L)).thenReturn(true);
        when(academicYearRepository.existsById(999L)).thenReturn(false);
        EvaluatePromotionsRequest req = new EvaluatePromotionsRequest(1L, 999L);
        ResponseEntity<ApiResponse<Void>> response = adminStageService.evaluatePromotions(req);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Academic year not found", response.getBody().getMessage());
    }

    @Test
    public void testEvaluatePromotions_ValidRequest_Success() {
        when(activityStageRepository.existsById(1L)).thenReturn(true);
        when(academicYearRepository.existsById(1L)).thenReturn(true);
        when(studentRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        EvaluatePromotionsRequest req = new EvaluatePromotionsRequest(1L, 1L);
        ResponseEntity<ApiResponse<Void>> response = adminStageService.evaluatePromotions(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }
}
