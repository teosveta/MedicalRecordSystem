package com.medicalrecord.controller;

import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
public class PatientForDoctorController {

    private final PatientService patientService;

    public PatientForDoctorController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }
}
