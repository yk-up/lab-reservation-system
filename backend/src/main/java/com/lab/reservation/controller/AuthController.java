package com.lab.reservation.controller;

import com.lab.reservation.dto.LoginRequest;
import com.lab.reservation.entity.User;
import com.lab.reservation.service.AuthService;
import com.lab.reservation.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        authService.register(user);
        return Result.success("注册成功");
    }
}
