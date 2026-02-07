package org.ecom.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank(message = "JWT secret must not be blank")
    @Size(min = 32, message = "JWT secret must be at least 32 characters")
    private String secret;

    private String previousSecrets = "";

    @NotNull
    @Min(value = 60000, message = "JWT expiration must be at least 60 seconds")
    private Long expirationMs;

    @NotNull
    @Min(value = 60000, message = "JWT refresh expiration must be at least 60 seconds")
    private Long refreshExpirationMs;
}
