package com.auth.authservice.services;

import com.auth.authservice.dto.responseDto.LoginResponseDto;
import com.auth.authservice.google.CodeStore;
import com.auth.authservice.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    @Autowired
    CodeStore codeStore;

    @Autowired
    JwtUtils jwtUtils;

    public LoginResponseDto getJwtFromAccessCode(String code){
        try{
            String token = codeStore.getToken("googleLogin", code);
            return LoginResponseDto.builder()
                    .success(true)
                    .message("Login successful")
                    .token(token)
                    .username(jwtUtils.extractUsername(token))
                    .expirationDate(jwtUtils.getExpiratonDate())
                    .build();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public LoginResponseDto getGmailTokens(String code){
        try{
            String token = codeStore.getToken("gmail", code);
            String[] tokens = token.split("_&&_");
            String accessToken = tokens[0];
            String refreshToken = tokens[1];
            String username = tokens[2];

            return LoginResponseDto.builder()
                    .success(true)
                    .message("Login successful")
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .username(username)
                    .expirationDate(jwtUtils.getExpiratonDate())
                    .build();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
