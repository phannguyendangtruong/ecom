package org.ecom.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ecom.dto.UserRequestDto;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username", unique = true, nullable = false)
    private String username;
    @Column(name ="password", nullable = false)
    private String password;
    @OneToOne
    @JoinColumn(name = "role_id")
    private Role role;
    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "email")
    private String email;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "provider")
    private String provider;

    
}
