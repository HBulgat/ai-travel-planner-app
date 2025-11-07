package top.bulgat.ai.travel.plan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.bulgat.ai.travel.plan.annotation.AuthRequired;
import top.bulgat.ai.travel.plan.model.User;
import top.bulgat.ai.travel.plan.model.dto.Resp;
import top.bulgat.ai.travel.plan.model.dto.UserLoginRequest;
import top.bulgat.ai.travel.plan.model.dto.UserLoginResponse;
import top.bulgat.ai.travel.plan.model.dto.UserProfileResponse;
import top.bulgat.ai.travel.plan.model.dto.UserRegisterRequest;
import top.bulgat.ai.travel.plan.service.IUserService;
import top.bulgat.ai.travel.plan.util.JwtUtil;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Resp<String> register(@RequestBody UserRegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        if (userService.registerUser(user)) {
            return Resp.<String>builder().code(200).message("User registered successfully").data("success").build();
        } else {
            return Resp.<String>builder().code(500).message("Registration failed").data("fail").build();
        }
    }

    @PostMapping("/login")
    public Resp<UserLoginResponse> login(@RequestBody UserLoginRequest request) {
        UserLoginResponse loginResponse = new UserLoginResponse();
        String token = userService.login(request.getUsername(), request.getPassword());
        if (token != null) {
            loginResponse.setToken(token);
            loginResponse.setMessage("Login successful");
            return Resp.<UserLoginResponse>builder().code(200).message("Login successful").data(loginResponse).build();
        }
        else {
            loginResponse.setMessage("Invalid credentials");
            return Resp.<UserLoginResponse>builder().code(401).message("Invalid credentials").data(loginResponse).build();
        }
    }

    @AuthRequired
    @GetMapping("/profile")
    public Resp<UserProfileResponse> getProfile(@RequestHeader("Authorization") String token) {
        String username = jwtUtil.extractUsername(token.substring(7));
        User user = userService.getUserByUsername(username);
        UserProfileResponse profileResponse = new UserProfileResponse();
        if (user != null) {
            profileResponse.setId(user.getId());
            profileResponse.setUsername(user.getUsername());
            profileResponse.setEmail(user.getEmail());
            return Resp.<UserProfileResponse>builder().code(200).message("Profile retrieved successfully").data(profileResponse).build();
        } else {
            return Resp.<UserProfileResponse>builder().code(404).message("User not found").build();
        }
    }
}