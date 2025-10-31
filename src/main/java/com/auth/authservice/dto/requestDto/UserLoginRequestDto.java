package com.auth.authservice.dto.requestDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginRequestDto {
    private String usernameOrEmail;
    private String password;
}
