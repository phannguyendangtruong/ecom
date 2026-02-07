package org.ecom.config;

import lombok.RequiredArgsConstructor;
import org.ecom.model.User;
import org.ecom.repository.UserRepository;
import org.ecom.service.RedisAuthService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
    private final UserRepository userRepository;
    private final RedisAuthService redisAuthService;
    @Override
    public UserDetails loadUserByUsername(String username){
        //check redis first
        UserDetails cached = redisAuthService.getUserDetailsFromCache(username);
        if(cached != null){
            return cached;
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        // Handle OAuth users who may not have passwords
        String password = user.getPassword() != null ? user.getPassword() : "{noop}";
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(password)
                .roles(user.getRole().getType())
                .build();
        redisAuthService.cacheUserDetails(username, userDetails);
        return userDetails;
    }
}
