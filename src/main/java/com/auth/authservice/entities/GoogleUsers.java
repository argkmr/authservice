package com.auth.authservice.entities;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "google-users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoogleUsers {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private LocalDate createdDate;

}
