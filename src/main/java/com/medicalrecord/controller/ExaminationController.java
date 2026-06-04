package com.medicalrecord.controller;

import com.medicalrecord.dto.examination.ExaminationRequest;
import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.examination.PatientNoteRequest;
import com.medicalrecord.service.ExaminationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examinations")
public class ExaminationController {

    private final ExaminationService examinationService;

    public ExaminationController(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    // ADMIN и DOCTOR виждат всички прегледи; PATIENT — само свои
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<List<ExaminationResponse>> getExaminations(Authentication authentication) {
        return ResponseEntity.ok(examinationService.getExaminations(authentication.getName()));
    }

    // Само лекарят може да създава прегледи — лекарят се взима от JWT
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ExaminationResponse> createExamination(
            @Valid @RequestBody ExaminationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(examinationService.createExamination(request, authentication.getName()));
    }

    // DOCTOR редактира само свои прегледи; ADMIN — всички
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ExaminationResponse> updateExamination(
            @PathVariable Long id,
            @Valid @RequestBody ExaminationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                examinationService.updateExamination(id, request, authentication.getName()));
    }

    // DOCTOR изтрива само свои прегледи; ADMIN — всички
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Void> deleteExamination(
            @PathVariable Long id,
            Authentication authentication) {
        examinationService.deleteExamination(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // Пациентът добавя или редактира бележка към свой преглед
    @PutMapping("/{id}/patient-note")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ExaminationResponse> updatePatientNote(
            @PathVariable Long id,
            @Valid @RequestBody PatientNoteRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                examinationService.updatePatientNote(id, request.getNote(), authentication.getName()));
    }
}
