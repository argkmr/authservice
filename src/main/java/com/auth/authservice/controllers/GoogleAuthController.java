package com.auth.authservice.controllers;

import com.auth.authservice.dto.BuildFailureResponse;
import com.auth.authservice.dto.responseDto.LoginResponseDto;
import com.auth.authservice.services.GoogleAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google")
public class GoogleAuthController {

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private BuildFailureResponse buildFailureResponse;

    @GetMapping("/login-success")
    public String loginSuccess(
            @RequestParam("jwt-access-code") String jwtCode,
            @RequestParam("gmail-access-code") String gmailCode) {
        return "Login Successful.\nJWT code: " + jwtCode + "\nGmail code: " + gmailCode;
    }

    @GetMapping("/jwt-token")
    public ResponseEntity<LoginResponseDto> getGoogleJwtToken(@RequestParam String code){
        try{
            LoginResponseDto tokenInfo = googleAuthService.getJwtFromAccessCode(code);
            if (tokenInfo.getToken() == null) {
                throw new RuntimeException("Something went wrong.");
            }
            return ResponseEntity.ok(tokenInfo);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildFailureResponse.loginBuildFailureResponse("", "google login failed"));
        }

    }

    @GetMapping("/gmail-tokens")
    public ResponseEntity<LoginResponseDto> getGmailTokens(@RequestParam String code){
        try{
            LoginResponseDto tokenInfo = googleAuthService.getGmailTokens(code);
            System.out.println("TokenInfo:" + tokenInfo);
            if (tokenInfo.getToken() == null) {
                throw new RuntimeException("Something went wrong.");
            }
            return ResponseEntity.ok(tokenInfo);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildFailureResponse.loginBuildFailureResponse("", "google login failed"));
        }
    }
}

