package com.auth.authservice.services;

import com.auth.authservice.entities.GoogleUsers;
import com.auth.authservice.entities.Users;
import com.auth.authservice.repository.GoogleUsersRepository;
import com.auth.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomGoogleUserDetailsService implements UserDetailsService {

    @Autowired
    GoogleUsersRepository googleUsersRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        GoogleUsers googleUser = googleUsersRepository.getByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with: " + email));

        return User.builder()
                .username(googleUser.getUsername())
                .roles(googleUser.getRole())
                .password("")
                .build();
    }
}
