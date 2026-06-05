package com.umiot.microclimate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class AuthService {

    private static final String USERS_FILE = "users.json";

    private record User(String username, String password, String role) {}

    private Map<String, User> users = new HashMap<>();

    @PostConstruct
    void loadUsers() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(USERS_FILE)) {
            Map<String, Object> root = new ObjectMapper().readValue(in, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, String>> list = (List<Map<String, String>>) root.get("users");
            for (Map<String, String> u : list) {
                User user = new User(u.get("username"), u.get("password"), u.getOrDefault("role", "user"));
                users.put(user.username, user);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users.json", e);
        }
    }

    public boolean validate(String username, String password) {
        User u = users.get(username);
        if (u == null) return false;
        return hash(password).equals(u.password);
    }

    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
