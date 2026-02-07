package org.ecom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ecom.validation.PasswordMatch;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@PasswordMatch
public class UserRequestDto {
    @Schema(example = "john.doe", description = "Public username")
    @NotNull(message = "Username cannot be null")
    @NotBlank(message = "Username cannot be blank")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]{3,50}$",
            message = "Username must be 3-50 characters and only contain letters, numbers, dot, underscore, or hyphen"
    )
    private String username;
    @Schema(example = "john.doe@example.com", description = "User email")
    @NotNull(message = "Email cannot be null")
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;
    @Schema(example = "Password1!", description = "Password meeting security policy")
    @NotNull(message = "Password cannot be null")
    @NotBlank(message = "Password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&/#^()_+=\\-]).{8,}$",
            message = "Password must be at least 8 characters and include uppercase, lowercase, number, and special character"
    )
    private String password;
    @Schema(example = "Password1!", description = "Must match password")
    @NotNull(message = "Confirm Password cannot be null")
    @NotBlank(message = "Confirm Password cannot be blank")
    private String confirmPassword;

}
