package com.lab.reservation.controller;

import com.lab.reservation.dto.LoginRequest;
import com.lab.reservation.entity.User;
import com.lab.reservation.service.AuthService;
import com.lab.reservation.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块", description = "迭代 1 - 登录与注册接口")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，成功后返回 JWT token 与用户信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": {
                                \"token\": \"eyJhbGciOiJIUzI1NiJ9...\",
                                \"userId\": 1,
                                \"username\": \"admin\",
                                \"realName\": \"系统管理员\",
                                \"role\": 1
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }

    @Operation(summary = "用户注册", description = "注册普通用户账号，注册成功后默认角色为普通用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功", content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              \"code\": 200,
                              \"message\": \"操作成功\",
                              \"data\": \"注册成功\"
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "用户名已存在")
    })
    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        authService.register(user);
        return Result.success("注册成功");
    }
}
