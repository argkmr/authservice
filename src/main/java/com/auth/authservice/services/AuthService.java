package com.auth.authservice.services;
import com.auth.authservice.dto.requestDto.UserLoginRequestDto;
import com.auth.authservice.dto.requestDto.UserRegistrationRequestDto;
import com.auth.authservice.dto.responseDto.LoginResponseDto;
import com.auth.authservice.entities.Role;
import com.auth.authservice.entities.Users;
import com.auth.authservice.enums.UserRole;
import com.auth.authservice.jwt.JwtUtils;
import com.auth.authservice.repository.RoleRepository;
import com.auth.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    CustomUserDetailsService userDetailsService;

    @Value("${app.default.role}")
    private String defaultRole;


    public Users register(UserRegistrationRequestDto userRegistrationRequestDto){

        Role userRole = saveRole();
        Integer userRoleId = userRole.getId();

        try{
            Users user = Users.builder()
                    .firstName(userRegistrationRequestDto.getFirstName())
                    .lastName(userRegistrationRequestDto.getLastName())
                    .username(userRegistrationRequestDto.getUsername())
                    .password(passwordEncoder.encode(userRegistrationRequestDto.getPassword()))
                    .email(userRegistrationRequestDto.getEmail())
                    .contactNumber(userRegistrationRequestDto.getContactNumber())
                    .createdDate(LocalDate.now())
                    .role(userRole)
                    .build();

            if(userRepository.existsByEmail(userRegistrationRequestDto.getEmail())){
                throw new UserAlreadyExistsException("User already exists");
            }
            return userRepository.save(user);
        }catch(UserAlreadyExistsException e){
            throw e;
        }catch(Exception e){
            roleRepository.deleteById(userRoleId);
            throw new RuntimeException("Registration failed due to an internal error");
        }
    }

    public Role saveRole(){
        try{
            UserRole enumRole = UserRole.valueOf(defaultRole);
            Role userRole = Role.builder()
                    .id(enumRole.ordinal())
                    .roleName(enumRole)
                    .build();
            return roleRepository.save(userRole);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public LoginResponseDto basicLogin(UserLoginRequestDto userLoginRequestDto){
        try{
            if(!validateUser(userLoginRequestDto.getPassword(), userLoginRequestDto.getUsernameOrEmail())){
                throw new IncorrectPasswordException("Incorrect credentials");
            }
            UserDetails user = userDetailsService.loadUserByUsername(userLoginRequestDto.getUsernameOrEmail());
            String token = jwtUtils.generateToken(user);
            return LoginResponseDto.builder()
                    .success(true)
                    .message("Login Successful")
                    .token(token)
                    .expirationDate(jwtUtils.getExpiratonDate())
                    .username(user.getUsername())
                    .build();

        }catch(IncorrectPasswordException e){
            throw e;
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean validateUser(String rawPassword, String usernameOrEmail) {
        // Fetch encoded password from DB
        String encodedPasswordFromDb = isEmail(usernameOrEmail)
                ? userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new RuntimeException("Password not found for email: " + usernameOrEmail))
                : userRepository.findByUsername(usernameOrEmail)
                .orElseThrow(() -> new RuntimeException("Password not found for username: " + usernameOrEmail));

        // Compare raw input password with encoded password from DB
        return passwordEncoder.matches(rawPassword, encodedPasswordFromDb);
    }

    private boolean isEmail(String usernameOrEmail){
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return usernameOrEmail.matches(emailRegex);
    }

    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class IncorrectPasswordException extends RuntimeException {
        public IncorrectPasswordException(String message) {
            super(message);
        }
    }
}
