package com.medicalrecord.controller;

import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.dto.statistics.DoctorCountResponse;
import com.medicalrecord.dto.statistics.MonthStatisticsResponse;
import com.medicalrecord.dto.statistics.PaymentByDoctorResponse;
import com.medicalrecord.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    // Пациенти с конкретна диагноза
    @GetMapping("/patients-by-diagnosis")
    public ResponseEntity<List<PatientResponse>> getPatientsByDiagnosis(
            @RequestParam Long diagnosisId) {
        return ResponseEntity.ok(statisticsService.getPatientsByDiagnosis(diagnosisId));
    }

    // Най-честа диагноза по брой прегледи
    @GetMapping("/most-common-diagnosis")
    public ResponseEntity<DiagnosisResponse> getMostCommonDiagnosis() {
        return ResponseEntity.ok(statisticsService.getMostCommonDiagnosis());
    }

    // Пациенти на конкретен лекар (личен лекар)
    @GetMapping("/patients-by-doctor")
    public ResponseEntity<List<PatientResponse>> getPatientsByDoctor(
            @RequestParam Long doctorId) {
        return ResponseEntity.ok(statisticsService.getPatientsByDoctor(doctorId));
    }

    // Обща сума платена от неосигурени пациенти
    @GetMapping("/total-patient-payments")
    public ResponseEntity<Map<String, BigDecimal>> getTotalPatientPayments() {
        return ResponseEntity.ok(Map.of("totalAmount", statisticsService.getTotalPatientPayments()));
    }

    // Платени суми по лекар
    @GetMapping("/patient-payments-by-doctor")
    public ResponseEntity<List<PaymentByDoctorResponse>> getPatientPaymentsByDoctor() {
        return ResponseEntity.ok(statisticsService.getPatientPaymentsByDoctor());
    }

    // Брой пациенти на всеки ОПЛ
    @GetMapping("/patients-count-per-gp")
    public ResponseEntity<List<DoctorCountResponse>> getPatientsCountPerGP() {
        return ResponseEntity.ok(statisticsService.getPatientsCountPerGP());
    }

    // Брой прегледи по лекар
    @GetMapping("/visits-per-doctor")
    public ResponseEntity<List<DoctorCountResponse>> getVisitsPerDoctor() {
        return ResponseEntity.ok(statisticsService.getVisitsPerDoctor());
    }

    // Прегледи на конкретен лекар в период
    @GetMapping("/examinations-by-doctor-and-period")
    public ResponseEntity<List<ExaminationResponse>> getExaminationsByDoctorAndPeriod(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                statisticsService.getExaminationsByDoctorAndPeriod(doctorId, from, to));
    }

    // Месецът с най-много болнични листове
    @GetMapping("/month-most-sick-leaves")
    public ResponseEntity<MonthStatisticsResponse> getMonthWithMostSickLeaves() {
        return ResponseEntity.ok(statisticsService.getMonthWithMostSickLeaves());
    }

    // Лекарят с най-много издадени болнични листове
    @GetMapping("/doctor-most-sick-leaves")
    public ResponseEntity<DoctorResponse> getDoctorWithMostSickLeaves() {
        return ResponseEntity.ok(statisticsService.getDoctorWithMostSickLeaves());
    }
}
