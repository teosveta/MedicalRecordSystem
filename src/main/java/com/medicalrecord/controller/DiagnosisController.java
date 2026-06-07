package com.medicalrecord.controller;

import com.medicalrecord.dto.diagnosis.DiagnosisRequest;
import com.medicalrecord.dto.diagnosis.DiagnosisResponse;
import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.service.DiagnosisService;
import com.medicalrecord.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final DoctorService doctorService;

    public DiagnosisController(DiagnosisService diagnosisService, DoctorService doctorService) {
        this.diagnosisService = diagnosisService;
        this.doctorService = doctorService;
    }

    // Всички роли имат достъп до списъка с диагнози
    @GetMapping("/api/diagnoses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiagnosisResponse>> getAllDiagnoses() {
        return ResponseEntity.ok(diagnosisService.getAllDiagnoses());
    }

    // Връща само диагнозите, релевантни за специалността на текущия лекар
    @GetMapping("/api/diagnoses/my-specialty")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<DiagnosisResponse>> getDiagnosesByMySpecialty(Authentication auth) {
        DoctorResponse doctor = doctorService.getDoctorByUsername(auth.getName());
        return ResponseEntity.ok(diagnosisService.getDiagnosesBySpecialties(doctor.getSpecialties()));
    }

    @PostMapping("/api/admin/diagnoses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DiagnosisResponse> createDiagnosis(@Valid @RequestBody DiagnosisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosisService.createDiagnosis(request));
    }

    @PutMapping("/api/admin/diagnoses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DiagnosisResponse> updateDiagnosis(
            @PathVariable Long id,
            @Valid @RequestBody DiagnosisRequest request) {
        return ResponseEntity.ok(diagnosisService.updateDiagnosis(id, request));
    }

    @DeleteMapping("/api/admin/diagnoses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDiagnosis(@PathVariable Long id) {
        diagnosisService.deleteDiagnosis(id);
        return ResponseEntity.noContent().build();
    }
}
