package com.demo.authservice.controller;

import com.demo.authservice.dto.AuthResponse;
import com.demo.authservice.dto.LoginRequest;
import com.demo.authservice.dto.RegisterRequest;
import com.demo.authservice.service.KeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakService keycloakService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("Login request for username={}", request.getUsername());
        return ResponseEntity.ok(keycloakService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        log.info("Register request for username={}", request.getUsername());
        keycloakService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleError(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Internal error";
        HttpStatus status = msg.contains("already exists") ? HttpStatus.CONFLICT : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(Map.of("error", msg));
    }
}
