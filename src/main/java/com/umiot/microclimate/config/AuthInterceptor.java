package com.umiot.microclimate.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();

        // 允许登录相关 API 和登录页面
        if (path.startsWith("/api/auth/") || path.equals("/login.html")) {
            return true;
        }

        // 允许静态资源
        String referer = request.getHeader("Referer");
        HttpSession session = request.getSession(false);
        boolean loggedIn = session != null && session.getAttribute("user") != null;

        // API 请求未登录 → 返回 401
        if (path.startsWith("/api/")) {
            if (!loggedIn) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Unauthorized\"}");
                return false;
            }
            return true;
        }

        // 页面请求未登录 → 重定向到登录页
        if (!loggedIn) {
            response.sendRedirect("/login.html");
            return false;
        }

        return true;
    }
}
