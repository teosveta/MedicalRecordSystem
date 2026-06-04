package com.medicalrecord.service;

import com.medicalrecord.dto.auth.LoginRequest;
import com.medicalrecord.dto.auth.LoginResponse;
import com.medicalrecord.dto.auth.RegisterRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);
}
