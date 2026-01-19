package org.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ecom.model.User;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserReponseDto {
    private String username;
    private String role;

    public UserReponseDto(User user){
        this.username = user.getUsername();
        this.role = user.getRole().getType();
    }
}
