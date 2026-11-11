package com.dineease.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry r) {
        r.addInterceptor(new AuthInterceptor()).addPathPatterns("/admin/**", "/dashboard", "/profile", "/reservations/**",
            "/reserve/confirm", "/reserve/done/**", "/waitlist/join", "/waitlist/*/cancel");
    }
}
