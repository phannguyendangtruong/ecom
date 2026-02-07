package org.ecom.service;

import org.ecom.dto.UserRequestDto;
import org.ecom.exception.BusinessException;
import org.ecom.mapper.UserMapper;
import org.ecom.model.Role;
import org.ecom.model.User;
import org.ecom.repository.RoleRepository;
import org.ecom.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends BaseServiceImpl<User, Long>{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    protected UserService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        super(userRepository);
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User save(User entity){
        // Only encode password if it's not null (OAuth users may not have passwords)
        if (entity.getPassword() != null && !entity.getPassword().isBlank()) {
            entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        return userRepository.save(entity);
    }
    public boolean existUsername(String username){
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean existEmail(String email){
        return userRepository.findByEmail(email).isPresent();
    }

    public User createUser(UserRequestDto userDto){
        if(!userDto.getPassword().equals(userDto.getConfirmPassword())){
            throw new BusinessException("Password and confirm password do not match", HttpStatus.BAD_REQUEST);
        }

        if(existUsername(userDto.getUsername())){
            throw new BusinessException("Username already exists", HttpStatus.BAD_REQUEST);
        }
        if(existEmail(userDto.getEmail())){
            throw new BusinessException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        // Public signup always receives USER role from server-side to prevent privilege escalation.
        Role role = roleRepository.findByType("USER")
                .orElseThrow(() -> new BusinessException("Default role USER not found", HttpStatus.INTERNAL_SERVER_ERROR));
        User user = userMapper.toEntity(userDto);
        user.setRole(role);
        return save(user);
    }

    public User updateUser(Long id, UserRequestDto userDto) {
        if(!userDto.getPassword().equals(userDto.getConfirmPassword())){
            throw new BusinessException("Password and confirm password do not match", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        if (!user.getUsername().equals(userDto.getUsername()) && existUsername(userDto.getUsername())) {
            throw new BusinessException("Username already exists", HttpStatus.BAD_REQUEST);
        }
        if (user.getEmail() != null && !user.getEmail().equals(userDto.getEmail()) && existEmail(userDto.getEmail())) {
            throw new BusinessException("Email already exists", HttpStatus.BAD_REQUEST);
        }
        if (user.getEmail() == null && existEmail(userDto.getEmail())) {
            throw new BusinessException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        // Only update password if provided (OAuth users may not have passwords)
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            user.setPassword(userDto.getPassword());
        }
        return save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("User not found", HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}
