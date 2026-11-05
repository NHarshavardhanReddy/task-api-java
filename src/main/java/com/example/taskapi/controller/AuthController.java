package com.example.taskapi.controller;

import com.example.taskapi.dto.LoginRequest;
import com.example.taskapi.dto.TokenResponse;
import com.example.taskapi.exception.UnauthorizedException;
import com.example.taskapi.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class AuthController {

    private final JwtService jwtService;
    private final String username;
    private final String password;

    public AuthController(JwtService jwtService,
                          @Value("${app.auth.username}") String username,
                          @Value("${app.auth.password}") String password) {
        this.jwtService = jwtService;
        this.username = username;
        this.password = password;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        // constant-time comparison to avoid timing leaks
        boolean userOk = MessageDigest.isEqual(bytes(req.username()), bytes(username));
        boolean passOk = MessageDigest.isEqual(bytes(req.password()), bytes(password));
        if (!(userOk && passOk)) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return new TokenResponse(jwtService.generateToken(req.username()), "bearer",
                jwtService.getExpirationSeconds());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
