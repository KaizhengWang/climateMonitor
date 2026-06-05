package com.umiot.microclimate.controller;

import com.umiot.microclimate.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_KEY = "user";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");
        Map<String, Object> result = new LinkedHashMap<>();
        if (authService.validate(username, password)) {
            session.setAttribute(SESSION_KEY, username);
            result.put("success", true);
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    @GetMapping("/check")
    public Map<String, Object> check(HttpSession session) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticated", session.getAttribute(SESSION_KEY) != null);
        return result;
    }
}
