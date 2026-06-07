package com.medicalrecord.controller;

import com.medicalrecord.dto.sickleave.SickLeaveRequest;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;
import com.medicalrecord.dto.sickleave.UpdateSickLeaveRequest;
import com.medicalrecord.service.SickLeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sick-leaves")
public class SickLeaveController {

    private final SickLeaveService sickLeaveService;

    public SickLeaveController(SickLeaveService sickLeaveService) {
        this.sickLeaveService = sickLeaveService;
    }

    // ADMIN и DOCTOR виждат всички болнични листове; PATIENT — само свои
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<List<SickLeaveResponse>> getSickLeaves(Authentication authentication) {
        return ResponseEntity.ok(sickLeaveService.getSickLeaves(authentication.getName()));
    }

    // Само лекарят издава болнични листове — лекарят и пациентът се взимат от прегледа
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<SickLeaveResponse> createSickLeave(
            @Valid @RequestBody SickLeaveRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sickLeaveService.createSickLeave(request, authentication.getName()));
    }

    // ADMIN редактира всеки болничен лист; DOCTOR — само свои
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<SickLeaveResponse> updateSickLeave(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSickLeaveRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(sickLeaveService.updateSickLeave(id, request, authentication.getName()));
    }

    // ADMIN изтрива всеки болничен лист; DOCTOR — само свои, само от днес
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<Void> deleteSickLeave(@PathVariable Long id, Authentication authentication) {
        sickLeaveService.deleteSickLeave(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
