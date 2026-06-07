package com.medicalrecord.controller;

import com.medicalrecord.dto.examination.ExaminationFeeResponse;
import com.medicalrecord.dto.examination.FeeOptionResponse;
import com.medicalrecord.enums.Specialty;
import com.medicalrecord.service.ExaminationFeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class ExaminationFeeController {

    private final ExaminationFeeService examinationFeeService;

    public ExaminationFeeController(ExaminationFeeService examinationFeeService) {
        this.examinationFeeService = examinationFeeService;
    }

    // ADMIN и DOCTOR виждат таксите — например при създаване на преглед
    @GetMapping("/api/examination-fees")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<List<ExaminationFeeResponse>> getAllFees() {
        return ResponseEntity.ok(examinationFeeService.getAllFees());
    }

    // Само ADMIN може да променя таксата за специалност
    @PutMapping("/api/admin/examination-fees/{specialty}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExaminationFeeResponse> updateFee(
            @PathVariable String specialty,
            @RequestBody Map<String, BigDecimal> body) {
        Specialty spec = Specialty.valueOf(specialty.toUpperCase());
        BigDecimal baseFee = body.get("baseFee");
        if (baseFee == null || baseFee.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Таксата трябва да бъде положително число");
        }
        return ResponseEntity.ok(examinationFeeService.updateFee(spec, baseFee));
    }

    // Таксите достъпни за текущия лекар — своята специалност, ОПЛ (ако canBeGP) и допълнителни услуги
    @GetMapping("/api/examination-fees/my-options")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<FeeOptionResponse>> getAvailableFees(Authentication authentication) {
        return ResponseEntity.ok(examinationFeeService.getAvailableFees(authentication.getName()));
    }
}
