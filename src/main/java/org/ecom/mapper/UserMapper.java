package org.ecom.mapper;

import org.ecom.dto.UserReponseDto;
import org.ecom.dto.UserRequestDto;
import org.ecom.model.Role;
import org.ecom.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "googleId", ignore = true)
    @Mapping(target = "provider", ignore = true)
    User toEntity(UserRequestDto dto);

    @Mapping(target = "role", source = "role.type")
    UserReponseDto toDto(User user);

    default User setRole(User user, Role role) {
        user.setRole(role);
        return user;
    }
}
