package com.auth.authservice.services;

import com.auth.authservice.entities.Users;
import com.auth.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {

        Users user = userRepository.getByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.getByEmail(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with: " + usernameOrEmail)));

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getRoleName().name())
                .build();
    }
}
