package com.govia.identity.config;

import com.govia.core.security.JwtAuthenticationFilter;
import com.govia.core.security.JwtTokenProvider;
import com.govia.core.security.UserSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cau hinh security dung chung: stateless JWT, khong session.
 * Cac module khac (khi tach rieng service) copy config nay hoac import tu govia-core sau nay.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Chi 2 endpoint nay thuc su khong can dang nhap. Liet ke tuong minh (khong dung wildcard
     * "/api/auth/**") de moi endpoint MOI them vao AuthController (vd doi mat khau) mac dinh
     * van yeu cau xac thuc, tranh vo tinh de lo endpoint nhay cam.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/login/resolve",
            "/api/auth/refresh",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            /* Handshake WebSocket khong the dinh kem header Authorization (gioi han cua browser
             * WebSocket API) - token duoc truyen qua query param va tu xac thuc rieng trong
             * WebSocketAuthInterceptor, KHONG phai bo qua xac thuc that su. */
            "/ws/**"
    };

    @Value("${govia.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Cho phep frontend dev server (Vite, cong 5173) goi API tu trinh duyet - khong co CORS, moi request se bi trinh duyet tu chan. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider tokenProvider,
                                            UserSessionService sessionService) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider, sessionService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
