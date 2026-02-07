package org.ecom.service;

import org.ecom.dto.UserRequestDto;
import org.ecom.exception.BusinessException;
import org.ecom.mapper.UserMapper;
import org.ecom.model.Role;
import org.ecom.model.User;
import org.ecom.repository.RoleRepository;
import org.ecom.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserAssignsDefaultUserRole() {
        UserRequestDto request = new UserRequestDto();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("Password1!");
        request.setConfirmPassword("Password1!");

        Role role = new Role();
        role.setType("USER");

        User entity = new User();
        entity.setUsername("alice");
        entity.setPassword("Password1!");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByType("USER")).thenReturn(Optional.of(role));
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");
        when(userRepository.save(entity)).thenReturn(entity);

        User saved = userService.createUser(request);

        assertEquals("USER", saved.getRole().getType());
        assertEquals("hashed-password", saved.getPassword());
    }

    @Test
    void createUserThrowsWhenDefaultRoleMissing() {
        UserRequestDto request = new UserRequestDto();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("Password1!");
        request.setConfirmPassword("Password1!");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByType("USER")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createUser(request));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    @Test
    void updateUserSuccess() {
        UserRequestDto request = new UserRequestDto();
        request.setUsername("alice.new");
        request.setEmail("alice.new@example.com");
        request.setPassword("Password1!");
        request.setConfirmPassword("Password1!");

        User existing = new User();
        existing.setId(1L);
        existing.setUsername("alice");
        existing.setEmail("alice@example.com");
        existing.setPassword("old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("alice.new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice.new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateUser(1L, request);

        assertEquals("alice.new", updated.getUsername());
        assertEquals("alice.new@example.com", updated.getEmail());
        assertEquals("hashed-password", updated.getPassword());
    }

    @Test
    void updateUserThrowsWhenNotFound() {
        UserRequestDto request = new UserRequestDto();
        request.setUsername("alice.new");
        request.setEmail("alice.new@example.com");
        request.setPassword("Password1!");
        request.setConfirmPassword("Password1!");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.updateUser(99L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
