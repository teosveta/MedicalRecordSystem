package com.medicalrecord.controller;

import com.medicalrecord.dto.doctor.DoctorRequest;
import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.dto.doctor.DoctorUpdateRequest;
import com.medicalrecord.enums.Specialty;
import com.medicalrecord.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {

    private final DoctorService doctorService;

    public AdminDoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // Връща списък с всички лекари
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    // Добавя нов лекар заедно с потребителски акаунт
    @PostMapping
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(request));
    }

    // Редактира данните на лекар (без имейл и парола)
    @PutMapping("/{id}")
    public ResponseEntity<DoctorResponse> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorUpdateRequest request) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, request));
    }

    // Изтрива лекар и свързания потребителски акаунт
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    // Връща всички специалности с техните имена — за динамично попълване на формата
    @GetMapping("/specialties")
    public ResponseEntity<List<Map<String, String>>> getSpecialties() {
        List<Map<String, String>> result = Arrays.stream(Specialty.values())
                .map(s -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("value", s.name());
                    entry.put("label", s.getLabel());
                    return entry;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
