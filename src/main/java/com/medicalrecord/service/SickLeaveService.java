package com.medicalrecord.service;

import com.medicalrecord.dto.sickleave.SickLeaveRequest;
import com.medicalrecord.dto.sickleave.SickLeaveResponse;

import java.util.List;

public interface SickLeaveService {

    List<SickLeaveResponse> getSickLeaves(String username);

    SickLeaveResponse createSickLeave(SickLeaveRequest request, String doctorUsername);

    void deleteSickLeave(Long id);
}
