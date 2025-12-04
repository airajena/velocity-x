package com.paypal.user_service.service;

import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.SignupRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.dto.JwtResponse;

public interface AuthService {
    String signup(SignupRequest request);
    JwtResponse login(LoginRequest request);
}
