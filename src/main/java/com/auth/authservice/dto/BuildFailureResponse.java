package com.auth.authservice.dto;

import com.auth.authservice.dto.requestDto.UserLoginRequestDto;
import com.auth.authservice.dto.requestDto.UserRegistrationRequestDto;
import com.auth.authservice.dto.responseDto.LoginResponseDto;
import com.auth.authservice.dto.responseDto.UserRegistrationMessage;
import com.auth.authservice.dto.responseDto.UserRegistrationResponseDto;
import org.springframework.stereotype.Component;

@Component
public class BuildFailureResponse {

    public UserRegistrationResponseDto registrationBuildFailureResponse(UserRegistrationRequestDto requestDto, String message) {
        return UserRegistrationResponseDto.builder()
                .id(null)
                .username(requestDto.getUsername())
                .email(requestDto.getEmail())
                .role(null)
                .data(UserRegistrationMessage.builder()
                        .message(message)
                        .createdDate(null)
                        .success(false)
                        .build())
                .build();
    }

    public LoginResponseDto loginBuildFailureResponse(String usernameOrEmail, String message) {
        return LoginResponseDto.builder()
                .success(false)
                .message("Something went wrong. Token might be expired.")
                .token(null)
                .expirationDate(null)
                .username(usernameOrEmail)
                .build();
    }

}
