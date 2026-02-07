package org.ecom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ecom.dto.UserReponseDto;
import org.ecom.dto.UserRequestDto;
import org.ecom.mapper.UserMapper;
import org.ecom.model.User;
import org.ecom.reponse.ApiResponse;
import org.ecom.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> get(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.ok(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND, "User not found")));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<UserReponseDto>> create(@RequestBody @Valid UserRequestDto userRequest){
        User user = userService.createUser(userRequest);
        UserReponseDto userReponseDto = userMapper.toDto(user);
        return ResponseEntity.ok(ApiResponse.ok(userReponseDto));
    }
}
