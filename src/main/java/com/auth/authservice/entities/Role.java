package com.auth.authservice.entities;

import com.auth.authservice.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Role {

    @Id
    @Column(nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private UserRole roleName;
}
