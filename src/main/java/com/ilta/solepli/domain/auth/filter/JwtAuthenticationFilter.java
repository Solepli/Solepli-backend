package com.ilta.solepli.domain.auth.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.user.util.CustomUserDetailService;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.util.JwtUtil;
import com.ilta.solepli.global.util.ResponseUtil;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final CustomUserDetailService customUserDetailService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      // 토큰이 있는 경우에만 검증 및 인증 처리
      String token = resolveToken(request);

      if (token != null) {
        jwtUtil.validateToken(token);
        String loginId = jwtUtil.getLoginIdFromToken(token);
        UserDetails userDetails = customUserDetailService.loadUserByUsername(loginId);

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }

      // 토큰이 없으면 인증 없이 넘어감 → permitAll이면 허용, 아니면 Security가 막음, 이때 JwtAuthenticationEntryPoint가 사용됨
      filterChain.doFilter(request, response);

    } catch (CustomException e) {
      ResponseUtil.writeError(response, e.errorCode);
    }
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
