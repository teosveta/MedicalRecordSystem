package com.medicalrecord.controller;

import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;
import com.medicalrecord.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
public class PatientMedicalRecordController {

    private final PatientService patientService;

    public PatientMedicalRecordController(PatientService patientService) {
        this.patientService = patientService;
    }

    // Всички прегледи на пациент — за медицинско досие
    @GetMapping("/examinations")
    public ResponseEntity<List<ExaminationResponse>> getPatientExaminations(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientExaminationsById(id));
    }

    // Всички болнични листове на пациент — за медицинско досие
    @GetMapping("/sick-leaves")
    public ResponseEntity<List<SickLeaveResponse>> getPatientSickLeaves(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientSickLeavesById(id));
    }
}
