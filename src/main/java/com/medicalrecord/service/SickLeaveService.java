package com.medicalrecord.service;

import com.medicalrecord.dto.sickleave.SickLeaveRequest;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;
import com.medicalrecord.dto.sickleave.UpdateSickLeaveRequest;

import java.util.List;

public interface SickLeaveService {

    List<SickLeaveResponse> getSickLeaves(String username);

    SickLeaveResponse createSickLeave(SickLeaveRequest request, String doctorUsername);

    SickLeaveResponse updateSickLeave(Long id, UpdateSickLeaveRequest request, String username);

    void deleteSickLeave(Long id, String username);
}
