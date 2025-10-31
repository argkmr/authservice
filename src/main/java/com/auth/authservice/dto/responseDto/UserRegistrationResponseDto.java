package com.auth.authservice.dto.responseDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRegistrationResponseDto {
    private String id;
    private String username;
    private String email;
    private String role;
    private UserRegistrationMessage data;
}

