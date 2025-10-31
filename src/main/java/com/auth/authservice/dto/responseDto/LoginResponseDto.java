package com.auth.authservice.dto.responseDto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class LoginResponseDto {
    private boolean success;
    private String message;
    private String username;
    private String token;
    private String refreshToken;
    private Date expirationDate;
}
