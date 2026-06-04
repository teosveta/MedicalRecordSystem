package com.medicalrecord.controller;

import com.medicalrecord.dto.examination.ExaminationResponse;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;
import com.medicalrecord.entity.Patient;
import com.medicalrecord.exception.ResourceNotFoundException;
import com.medicalrecord.mapper.ExaminationMapper;
import com.medicalrecord.mapper.SickLeaveMapper;
import com.medicalrecord.repository.ExaminationRepository;
import com.medicalrecord.repository.PatientRepository;
import com.medicalrecord.repository.SickLeaveRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
public class PatientMedicalRecordController {

    private final PatientRepository patientRepository;
    private final ExaminationRepository examinationRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final ExaminationMapper examinationMapper;
    private final SickLeaveMapper sickLeaveMapper;

    public PatientMedicalRecordController(PatientRepository patientRepository,
                                          ExaminationRepository examinationRepository,
                                          SickLeaveRepository sickLeaveRepository,
                                          ExaminationMapper examinationMapper,
                                          SickLeaveMapper sickLeaveMapper) {
        this.patientRepository = patientRepository;
        this.examinationRepository = examinationRepository;
        this.sickLeaveRepository = sickLeaveRepository;
        this.examinationMapper = examinationMapper;
        this.sickLeaveMapper = sickLeaveMapper;
    }

    // Всички прегледи на пациент — за медицинско досие
    @GetMapping("/examinations")
    public ResponseEntity<List<ExaminationResponse>> getPatientExaminations(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", id));
        List<ExaminationResponse> examinations = examinationRepository.findByPatient(patient).stream()
                .map(examinationMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(examinations);
    }

    // Всички болнични листове на пациент — за медицинско досие
    @GetMapping("/sick-leaves")
    public ResponseEntity<List<SickLeaveResponse>> getPatientSickLeaves(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пациент", "id", id));
        List<SickLeaveResponse> sickLeaves = sickLeaveRepository.findByPatient(patient).stream()
                .map(sickLeaveMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sickLeaves);
    }
}
