package com.auth.authservice.controllers;

import com.auth.authservice.dto.BuildFailureResponse;
import com.auth.authservice.dto.requestDto.UserLoginRequestDto;
import com.auth.authservice.dto.requestDto.UserRegistrationRequestDto;
import com.auth.authservice.dto.responseDto.LoginResponseDto;
import com.auth.authservice.dto.responseDto.UserRegistrationMessage;
import com.auth.authservice.dto.responseDto.UserRegistrationResponseDto;
import com.auth.authservice.entities.Users;
import com.auth.authservice.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
public class BasicAuthController {

    @Autowired
    AuthService authService;

    @Autowired
    BuildFailureResponse buildFailureResponse;

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDto> userRegistration(
            @RequestBody UserRegistrationRequestDto userRegistrationRequestDto) {

        try {
            Users userInfo = authService.register(userRegistrationRequestDto);
            UserRegistrationMessage message = UserRegistrationMessage.builder()
                    .message("User has been registered")
                    .createdDate(userInfo.getCreatedDate())
                    .success(true)
                    .build();

            UserRegistrationResponseDto response =  UserRegistrationResponseDto.builder()
                    .id(userInfo.getId().toString())
                    .username(userInfo.getUsername())
                    .email(userInfo.getEmail())
                    .role(userInfo.getRole().getRoleName().name())
                    .data(message)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (AuthService.UserAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildFailureResponse.registrationBuildFailureResponse(userRegistrationRequestDto, "User already exists"));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildFailureResponse.registrationBuildFailureResponse(userRegistrationRequestDto, "Registration failed. Please try again."));
        }
    }

    @PostMapping("/basic-login")
    public ResponseEntity<LoginResponseDto> userBasicLogin(@RequestBody UserLoginRequestDto userLoginRequestDto){
        try{
            LoginResponseDto response = authService.basicLogin(userLoginRequestDto);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(buildFailureResponse.loginBuildFailureResponse(userLoginRequestDto.getUsernameOrEmail(), "Login failed"));
        }
    }

}
