package com.sgsbarber.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns(
                                "http://127.0.0.1:5500", "http://localhost:5500", "http://localhost:3000",
                                "http://192.168.*:5500", "http://10.*:5500", "http://172.16.*:5500",
                                "http://172.17.*:5500", "http://172.18.*:5500", "http://172.19.*:5500",
                                "http://172.20.*:5500", "http://172.21.*:5500", "http://172.22.*:5500",
                                "http://172.23.*:5500", "http://172.24.*:5500", "http://172.25.*:5500",
                                "http://172.26.*:5500", "http://172.27.*:5500", "http://172.28.*:5500",
                                "http://172.29.*:5500", "http://172.30.*:5500", "http://172.31.*:5500")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
