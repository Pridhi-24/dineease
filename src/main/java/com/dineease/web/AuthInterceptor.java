package com.dineease.web;

import com.dineease.model.User;
import jakarta.servlet.http.*;
import org.springframework.web.servlet.HandlerInterceptor;

/** Role-based access: customers must be logged in, /admin/** needs the admin role. */
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        HttpSession s = req.getSession();
        User u = (User) s.getAttribute("user");
        String uri = req.getRequestURI();
        if (u == null) {
            String next = "GET".equals(req.getMethod()) ? uri
                : uri.startsWith("/reserve") ? "/reserve/summary" : uri.startsWith("/waitlist") ? "/waitlist" : "/dashboard";
            s.setAttribute("next", next);
            res.sendRedirect("/login");
            return false;
        }
        if (uri.startsWith("/admin") && !"admin".equals(u.role)) { res.sendRedirect("/"); return false; }
        return true;
    }
}
