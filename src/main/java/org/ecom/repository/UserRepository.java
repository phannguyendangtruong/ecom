package org.ecom.repository;

import org.ecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByRefreshToken(String refreshToken);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);
}
