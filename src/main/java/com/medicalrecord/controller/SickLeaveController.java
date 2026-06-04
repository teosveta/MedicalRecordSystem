package com.medicalrecord.controller;

import com.medicalrecord.dto.sickleave.SickLeaveRequest;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;
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

    // Само администраторът може да изтрива болнични листове
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSickLeave(@PathVariable Long id) {
        sickLeaveService.deleteSickLeave(id);
        return ResponseEntity.noContent().build();
    }
}
