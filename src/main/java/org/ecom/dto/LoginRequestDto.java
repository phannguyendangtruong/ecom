package org.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    @NotBlank (message = "username can not be blank")
    private String username;
    @NotBlank (message = "password can not be blank")
    private String password;
}
