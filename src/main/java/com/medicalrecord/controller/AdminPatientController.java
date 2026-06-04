package com.medicalrecord.controller;

import com.medicalrecord.dto.patient.PatientRequest;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/patients")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPatientController {

    private final PatientService patientService;

    public AdminPatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // Връща всички пациенти
    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    // Добавя нов пациент заедно с потребителски акаунт
    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createPatient(request));
    }

    // Редактира данните на пациент
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(patientService.updatePatient(id, request));
    }

    // Изтрива пациент и свързания потребителски акаунт
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    // Назначава личен лекар на пациент
    @PutMapping("/{id}/assign-doctor/{doctorId}")
    public ResponseEntity<PatientResponse> assignDoctor(
            @PathVariable Long id,
            @PathVariable Long doctorId) {
        return ResponseEntity.ok(patientService.assignPersonalDoctor(id, doctorId));
    }
}
