package com.auth.authservice.entities;

import com.auth.authservice.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String firstName;

    private String lastName;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private LocalDate createdDate;

    @Column(length = 15, nullable = true)
    private String contactNumber;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

}
