package com.demo.authservice.service;

import com.demo.authservice.config.KeycloakProperties;
import com.demo.authservice.dto.AuthResponse;
import com.demo.authservice.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final KeycloakProperties keycloakProperties;
    private final RestTemplate restTemplate;

    public AuthResponse login(String username, String password) {
        String tokenUrl = keycloakProperties.getUrl()
                + "/realms/" + keycloakProperties.getRealm()
                + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakProperties.getClientId());
        body.add("client_secret", keycloakProperties.getClientSecret());
        body.add("username", username);
        body.add("password", password);

        try {
            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                    tokenUrl, new HttpEntity<>(body, headers), AuthResponse.class);
            log.info("Login successful for username={}", username);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Login failed for username={}: status={}", username, e.getStatusCode());
            throw new RuntimeException("Authentication failed", e);
        }
    }

    public void register(RegisterRequest req) {
        String adminToken = getAdminToken();

        String createUserUrl = keycloakProperties.getUrl()
                + "/admin/realms/" + keycloakProperties.getRealm()
                + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> userRepresentation = new HashMap<>();
        userRepresentation.put("username", req.getUsername());
        userRepresentation.put("email", req.getEmail());
        userRepresentation.put("firstName", req.getFirstName());
        userRepresentation.put("lastName", req.getLastName());
        userRepresentation.put("enabled", true);
        userRepresentation.put("credentials", List.of(
                Map.of("type", "password", "value", req.getPassword(), "temporary", false)
        ));

        try {
            restTemplate.postForEntity(createUserUrl, new HttpEntity<>(userRepresentation, headers), Void.class);
            log.info("User registered successfully: username={}", req.getUsername());
        } catch (HttpClientErrorException e) {
            log.error("Registration failed for username={}: status={}", req.getUsername(), e.getStatusCode());
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new RuntimeException("User already exists: " + req.getUsername(), e);
            }
            throw new RuntimeException("Registration failed", e);
        }
    }

    private String getAdminToken() {
        String masterTokenUrl = keycloakProperties.getUrl()
                + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", keycloakProperties.getAdminUsername());
        body.add("password", keycloakProperties.getAdminPassword());

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    masterTokenUrl, new HttpEntity<>(body, headers), Map.class);
            return (String) response.getBody().get("access_token");
        } catch (HttpClientErrorException e) {
            log.error("Failed to get admin token: status={}", e.getStatusCode());
            throw new RuntimeException("Failed to acquire admin token", e);
        }
    }
}
