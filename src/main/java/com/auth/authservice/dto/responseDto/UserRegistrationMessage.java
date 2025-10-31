package com.auth.authservice.dto.responseDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserRegistrationMessage {
    private Boolean success;
    private LocalDate createdDate;
    private String message;
}
