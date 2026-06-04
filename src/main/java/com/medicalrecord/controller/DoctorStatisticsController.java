package com.medicalrecord.controller;

import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics/my")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorStatisticsController {

    private final StatisticsService statisticsService;

    public DoctorStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/visits-count")
    public ResponseEntity<Map<String, Long>> getMyVisitsCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", statisticsService.getMyVisitsCount(auth.getName())));
    }

    @GetMapping("/patients-count")
    public ResponseEntity<Map<String, Long>> getMyPatientsCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", statisticsService.getMyPatientsCount(auth.getName())));
    }

    @GetMapping("/total-revenue")
    public ResponseEntity<Map<String, BigDecimal>> getMyTotalRevenue(Authentication auth) {
        return ResponseEntity.ok(Map.of("totalAmount", statisticsService.getMyTotalRevenue(auth.getName())));
    }

    @GetMapping("/patient-payments")
    public ResponseEntity<Map<String, BigDecimal>> getMyPatientPayments(Authentication auth) {
        return ResponseEntity.ok(Map.of("totalAmount", statisticsService.getMyPatientPayments(auth.getName())));
    }

    @GetMapping("/most-common-diagnosis")
    public ResponseEntity<DiagnosisResponse> getMyMostCommonDiagnosis(Authentication auth) {
        return ResponseEntity.ok(statisticsService.getMyMostCommonDiagnosis(auth.getName()));
    }

    @GetMapping("/sick-leaves-count")
    public ResponseEntity<Map<String, Long>> getMySickLeavesCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", statisticsService.getMySickLeavesCount(auth.getName())));
    }

    @GetMapping("/examinations-by-period")
    public ResponseEntity<List<ExaminationResponse>> getMyExaminationsByPeriod(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(statisticsService.getMyExaminationsByPeriod(auth.getName(), from, to));
    }

    @GetMapping("/my-patients-by-diagnosis")
    public ResponseEntity<List<PatientResponse>> getMyPatientsByDiagnosis(
            Authentication auth,
            @RequestParam Long diagnosisId) {
        return ResponseEntity.ok(statisticsService.getMyPatientsByDiagnosis(auth.getName(), diagnosisId));
    }
}
