package com.sgsbarber.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/services", "/api/services/**",
                                "/api/barbers", "/api/barbers/**", "/api/ratings", "/api/ratings/**",
                                "/api/businessProfile").permitAll()
                        .requestMatchers("/api/dashboard/**", "/api/users/**", "/api/clients/**",
                                "/api/waitlist/**", "/api/notifications/**").hasAnyRole("BARBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/appointments/**").hasAnyRole("CLIENT", "BARBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/barbers/**", "/api/services/**",
                                "/api/products/**", "/api/blocks/**", "/api/barberSchedules/**",
                                "/api/businessProfile/**", "/api/barberServices/**",
                                "/api/appointmentProducts/**").hasAnyRole("BARBER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/appointments/**",
                                "/api/services/**", "/api/products/**", "/api/blocks/**",
                                "/api/barberSchedules/**", "/api/businessProfile/**",
                                "/api/barberServices/**", "/api/appointmentProducts/**")
                                .hasAnyRole("BARBER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/appointments/**",
                                "/api/services/**", "/api/products/**", "/api/blocks/**",
                                "/api/barberSchedules/**", "/api/businessProfile/**",
                                "/api/barberServices/**", "/api/appointmentProducts/**")
                                .hasAnyRole("BARBER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        return http.build();
    }
}
