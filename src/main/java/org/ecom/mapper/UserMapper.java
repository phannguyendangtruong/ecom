package org.ecom.mapper;

import org.ecom.dto.UserResponseDto;
import org.ecom.dto.UserRequestDto;
import org.ecom.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "googleId", ignore = true)
    @Mapping(target = "provider", ignore = true)
    User toEntity(UserRequestDto dto);

    @Mapping(target = "role", source = "role.type")
    UserResponseDto toDto(User user);
}
