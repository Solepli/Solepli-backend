package com.ilta.solepli.global.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.auth.filter.JwtAuthenticationEntryPoint;
import com.ilta.solepli.domain.auth.filter.JwtAuthenticationFilter;
import com.ilta.solepli.domain.user.util.CustomUserDetailService;
import com.ilta.solepli.global.exception.handler.JwtAccessDeniedHandler;
import com.ilta.solepli.global.util.JwtUtil;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // @PreAuthorize 사용 가능하게 함
@EnableWebSecurity
public class SecurityConfig {

  private final JwtUtil jwtUtil;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
  private final CustomUserDetailService customUserDetailService;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable()) // JWT 기반 인증이라 CSRF 비활성화
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(jwtAccessDeniedHandler))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/swagger",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs",
                        "/api-docs/**",
                        "/v3/api-docs/**",
                        "/health/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/auth/**",
                        "/api/solmap/markers",
                        "/api/sollect/search",
                        "/api/solmap/places",
                        "/api/solmap/search/related",
                        "/api/sollect/search/place/**",
                        "/api/solmap/region/*/markers",
                        "/api/solmap/region/*/places",
                        "/api/solmap/place/search/*",
                        "/api/sollect/popular",
                        "/api/sollect/related/*",
                        "/api/solmap/places/nearby",
                        "/api/solmap/place/*/reviews",
                        "/api/place/search",
                        "/api/solroute/place/nearby/*",
                        "/api/solroute/place/*",
                        "/api/sollect/recommend")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/sollect/*")
                    .permitAll()
                    .anyRequest()
                    .authenticated() // 그 외 요청은 인증 필요
            )
        // CORS 설정을 수동으로 추가
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtUtil, customUserDetailService),
            UsernamePasswordAuthenticationFilter.class) // JWT 필터 추가
        .build();
  }

  // CORS 설정을 위한 Bean 정의
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(
        Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080",
            "http://localhost:5173")); // 추후 배포 시 변경 필요
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        Arrays.asList("X-Requested-With", "Content-Type", "Authorization", "X-XSRF-token"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 CORS 설정 적용
    return source;
  }
}
