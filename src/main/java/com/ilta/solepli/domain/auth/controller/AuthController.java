package com.ilta.solepli.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.auth.dto.request.BasicLoginRequest;
import com.ilta.solepli.domain.auth.dto.response.LoginResponse;
import com.ilta.solepli.domain.auth.service.AuthService;
import com.ilta.solepli.global.response.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "AuthController", description = "인증 및 회원 관련 API")
public class AuthController {

  private final AuthService authService;

  @Operation(
      summary = "기본 회원가입 API",
      description = "(관리자) 아이디와 비밀번호로로 회원가입합니다. 로그인에 사용할 아이디와 비밀번호를 입력해주세요.")
  @PostMapping("/signup")
  public ResponseEntity<SuccessResponse<Void>> signUp(
      @Valid @RequestBody BasicLoginRequest basicLoginRequest) {

    authService.signup(basicLoginRequest);
    return ResponseEntity.ok().body(SuccessResponse.successWithNoData("회원가입 성공"));
  }

  @Operation(
      summary = "기본 로그인 API",
      description = "(관리자) 아이디와 비밀번호로 로그인합니다. 로그인에 사용할 아이디와 비밀번호를 입력해주세요.")
  @PostMapping("/login")
  public ResponseEntity<SuccessResponse<LoginResponse>> login(
      @Valid @RequestBody BasicLoginRequest basicLoginRequest, HttpServletResponse response) {

    LoginResponse loginResponse = authService.login(basicLoginRequest, response);
    return ResponseEntity.ok().body(SuccessResponse.successWithData(loginResponse));
  }

  @Operation(summary = "소셜 로그인 API", description = "소셜 로그인을 진행합니다. (카카오, 네이버, 구글) 인가코드를 넣어주세요.")
  @GetMapping("/login/{loginType}")
  public ResponseEntity<SuccessResponse<LoginResponse>> login(
      @PathVariable String loginType, @RequestParam String code, HttpServletResponse response) {

    LoginResponse loginResponse = authService.socialLogin(code, loginType, response);
    return ResponseEntity.ok().body(SuccessResponse.successWithData(loginResponse));
  }
}
