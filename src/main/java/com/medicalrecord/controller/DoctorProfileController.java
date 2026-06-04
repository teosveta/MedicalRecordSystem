package com.medicalrecord.controller;

import com.medicalrecord.dto.doctor.ChangePasswordRequest;
import com.medicalrecord.dto.doctor.DoctorResponse;
import com.medicalrecord.dto.patient.PatientResponse;
import com.medicalrecord.enums.Specialty;
import com.medicalrecord.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
public class DoctorProfileController {

    private final DoctorService doctorService;

    public DoctorProfileController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // Публичен списък с ОПЛ лекари — използва се при регистрация на пациент
    @GetMapping("/gp")
    public ResponseEntity<List<DoctorResponse>> getGpDoctors() {
        return ResponseEntity.ok(doctorService.getGpDoctors());
    }

    // Списък с всички специалности — достъпен за DOCTOR и ADMIN
    @GetMapping("/specialties")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
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

    // Профилът на текущия лекар — използва се от UI за извличане на doctorId
    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(doctorService.getDoctorByUsername(authentication.getName()));
    }

    // Пациентите на текущия лекар (personalDoctor = me)
    @GetMapping("/my-patients")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<PatientResponse>> getMyPatients(Authentication authentication) {
        return ResponseEntity.ok(doctorService.getMyPatients(authentication.getName()));
    }

    // Лекарят сменя собствената си парола
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        doctorService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }
}
