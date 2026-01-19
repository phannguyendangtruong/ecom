package org.ecom.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.ecom.model.Role;
import org.ecom.model.User;
import org.ecom.repository.RoleRepository;
import org.ecom.repository.UserRepository;
import org.ecom.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final RedisAuthService redisAuthService;

    @Value("${google.oauth.client-id}")
    private String clientId;

    public User verifyGoogleToken(String idTokenString) throws GeneralSecurityException, IOException{
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if(idToken != null){
            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            //tìm user theo googleid hoac email
            User user = userRepository.findByGoogleId(googleId).orElse(userRepository.findByEmail(email).orElse(null));
            if(user == null){
                user = new User();
                user.setGoogleId(googleId);
                user.setEmail(email);
                user.setUsername(email); // Dùng email làm username
                user.setProvider("google");

                Role defaultRole = roleRepository.findByType("USER")
                        .orElseThrow(() -> new RuntimeException("Default role USER not found"));
                user.setRole(defaultRole);
                user = userRepository.save(user);
            }else{
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                }
                if (user.getEmail() == null) {
                    user.setEmail(email);
                }
                user.setProvider("google");
                user = userRepository.save(user);
            }
            return user;
        }else {
            throw new RuntimeException("Invalid Google ID token");
        }
    }
}
