package org.ecom.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ecom.dto.UserResponseDto;
import org.ecom.dto.UserRequestDto;
import org.ecom.mapper.UserMapper;
import org.ecom.response.ApiResponse;
import org.ecom.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/user", "/api/v1/user"})
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    @Operation(summary = "List users", description = "Get paginated list of users")
    public ResponseEntity<ApiResponse<Page<UserResponseDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<UserResponseDto> users = userService.findAll(PageRequest.of(page, size))
                .map(userMapper::toDto);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user", description = "Get user details by id")
    public ResponseEntity<ApiResponse<UserResponseDto>> get(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.ok(userMapper.toDto(user))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND, "User not found")));
    }

    @PostMapping("/create")
    @Operation(summary = "Create user", description = "Register a new user with USER role")
    public ResponseEntity<ApiResponse<UserResponseDto>> create(@RequestBody @Valid UserRequestDto userRequest){
        UserResponseDto userResponseDto = userMapper.toDto(userService.createUser(userRequest));
        return ResponseEntity.ok(ApiResponse.ok(userResponseDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update username/email/password for an existing user")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(@PathVariable Long id, @RequestBody @Valid UserRequestDto userRequest){
        UserResponseDto userResponseDto = userMapper.toDto(userService.updateUser(id, userRequest));
        return ResponseEntity.ok(ApiResponse.ok(userResponseDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete user by id")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.okMessage("User deleted"));
    }
}
