package org.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDto {
    @NotNull(message = "Username cannot be null")
    @NotBlank(message = "Username cannot be blank")
    private String username;
    @NotNull(message = "Password cannot be null")
    @NotBlank(message = "Password cannot be blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&/#^()_+=\\-]).{6,}$",
            message = "Password must be at least 6 characters and include uppercase, lowercase, number, and special character"
    )
    private String password;
    @NotNull(message = "Confirm Password cannot be null")
    @NotBlank(message = "Confirm Password cannot be blank")
    private String confirmPassword;
    @NotNull(message = "Role cannot be null")
    private long roleId;

}
