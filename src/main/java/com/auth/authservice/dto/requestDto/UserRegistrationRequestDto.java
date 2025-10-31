package com.auth.authservice.dto.requestDto;

import lombok.*;

@Data
@Builder
public class UserRegistrationRequestDto {
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String email;
    private String contactNumber;
}
