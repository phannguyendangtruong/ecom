package org.ecom.service;

import org.ecom.dto.UserRequestDto;
import org.ecom.exception.BusinessException;
import org.ecom.mapper.UserMapper;
import org.ecom.model.Role;
import org.ecom.model.User;
import org.ecom.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends BaseServiceImpl<User, Long>{
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    protected UserService(UserRepository userRepository, RoleService roleService, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        super(userRepository);
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User save(User entity){
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        return userRepository.save(entity);
    }
    public boolean existUsername(String username){
        return userRepository.findByUsername(username).isPresent();
    }

    public User createUser(UserRequestDto userDto){
        if(!userDto.getPassword().equals(userDto.getConfirmPassword())){
            throw new BusinessException("Password and confirm password do not match", HttpStatus.BAD_REQUEST);
        }

        if(existUsername(userDto.getUsername())){
            throw new BusinessException("Username already exists", HttpStatus.BAD_REQUEST);
        }

        Role role = roleService.findById(userDto.getRoleId()).
                orElseThrow(() -> new BusinessException("Role not found", HttpStatus.BAD_REQUEST));
        User user = userMapper.toEntity(userDto);
        user.setRole(role);
        return save(user);
    }
}
