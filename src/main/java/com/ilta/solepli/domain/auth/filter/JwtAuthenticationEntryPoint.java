package com.ilta.solepli.domain.auth.filter;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.util.ResponseUtil;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    ResponseUtil.writeError(response, ErrorCode.UNAUTHORIZED);
  }
}
