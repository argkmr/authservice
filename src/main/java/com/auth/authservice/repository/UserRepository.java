package com.auth.authservice.repository;

import com.auth.authservice.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<Users> getByUsername(String username);
    Optional<Users> getByEmail(String email);

    @Query("SELECT u.password FROM Users u WHERE u.username = :username")
    Optional<String> findByUsername(@Param("username") String username);

    @Query("SELECT u.password FROM Users u WHERE u.email = :email")
    Optional<String> findByEmail(@Param("email") String email);
}

