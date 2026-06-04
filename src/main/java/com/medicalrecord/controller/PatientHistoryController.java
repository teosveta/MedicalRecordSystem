package com.medicalrecord.controller;

import com.medicalrecord.dto.patient.PatientHistoryResponse;
import com.medicalrecord.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
public class PatientHistoryController {

    private final PatientService patientService;

    public PatientHistoryController(PatientService patientService) {
        this.patientService = patientService;
    }

    // Пациентът преглежда собствената си медицинска история
    @GetMapping("/history")
    public ResponseEntity<PatientHistoryResponse> getHistory(Authentication authentication) {
        return ResponseEntity.ok(patientService.getPatientHistory(authentication.getName()));
    }
}
