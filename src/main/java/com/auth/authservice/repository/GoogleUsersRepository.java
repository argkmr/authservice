package com.auth.authservice.repository;

import com.auth.authservice.entities.GoogleUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoogleUsersRepository extends JpaRepository<GoogleUsers, UUID> {
    public Optional<GoogleUsers> getByEmail(String email);
}
