package com.ilta.solepli.domain.auth.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.auth.dto.request.BasicLoginRequest;
import com.ilta.solepli.domain.auth.dto.response.LoginResponse;
import com.ilta.solepli.domain.auth.entity.LoginType;
import com.ilta.solepli.domain.auth.service.oauth.OAuthService;
import com.ilta.solepli.domain.auth.service.oauth.OAuthServiceFactory;
import com.ilta.solepli.domain.solmark.place.entity.SolmarkPlaceCollection;
import com.ilta.solepli.domain.solmark.place.repository.SolmarkPlaceCollectionRepository;
import com.ilta.solepli.domain.user.entity.Role;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.repository.UserRepository;
import com.ilta.solepli.domain.user.service.UserService;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.util.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final OAuthServiceFactory oauthServiceFactory;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final SolmarkPlaceCollectionRepository solmarkPlaceCollectionRepository;
  private final RedisTemplate<String, String> redisTemplate;
  private final JwtUtil jwtUtil;

  @Value("${DEFAULT_PROFILE_URL}")
  private String defaultImageUrl;

  @Value("${spring.jwt.refresh.token.expiration}")
  private long refreshTokenExpiration;

  @Value("${FRONTEND_DOMAIN}")
  private String frontendDomain;

  private static final String REFRESH_PREFIX = "refresh:";

  @Transactional
  public void signup(BasicLoginRequest request) {
    String loginId = request.loginId();

    if (userRepository.existsByLoginId(loginId)) {
      throw new CustomException(ErrorCode.USER_EXISTS);
    }

    User savedUser =
        userRepository.save(
            User.builder()
                .role(Role.ADMIN)
                .loginId(loginId)
                .password(passwordEncoder.encode(request.password()))
                .profileImageUrl(defaultImageUrl)
                .nickname(userService.generateAdminNickname())
                .loginType(LoginType.BASIC)
                .build());

    SolmarkPlaceCollection solmarkPlaceCollection =
        SolmarkPlaceCollection.builder().name("저장 리스트").iconId(1).user(savedUser).build();

    solmarkPlaceCollectionRepository.save(solmarkPlaceCollection);
  }

  @Transactional(readOnly = true)
  public LoginResponse login(BasicLoginRequest request, HttpServletResponse response) {
    User user =
        userRepository
            .findByLoginId(request.loginId())
            .orElseThrow(() -> new CustomException(ErrorCode.INCORRECT_ACCOUNT));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new CustomException(ErrorCode.INCORRECT_ACCOUNT);
    }

    String accessToken = jwtTokenProvider.generateAccessToken(user);
    String refreshToken = jwtTokenProvider.generateRefreshToken(user);

    // Redis에 Refresh Token 저장
    redisTemplate
        .opsForValue()
        .set(
            REFRESH_PREFIX + user.getLoginId(),
            refreshToken,
            Duration.ofMillis(refreshTokenExpiration));

    // 쿠키로 Refresh Token 전달
    addRefreshTokenToCookie(response, refreshToken);

    return LoginResponse.from(user.getId(), accessToken, user.getRole());
  }

  @Transactional
  public LoginResponse socialLogin(String code, String input, HttpServletResponse response) {
    LoginType loginType;
    try {
      loginType = LoginType.valueOf(input);
    } catch (IllegalStateException e) {
      throw new CustomException(ErrorCode.INCORRECT_LOGIN_TYPE);
    }

    OAuthService oauthService = oauthServiceFactory.getOAuthService(loginType);

    String loginId = oauthService.getLoginId(oauthService.getAccessToken(code));

    User user = userService.findOrSignUpUser(loginId, loginType);

    String accessToken = jwtTokenProvider.generateAccessToken(user);
    String refreshToken = jwtTokenProvider.generateRefreshToken(user);

    // Redis에 Refresh Token 저장
    redisTemplate
        .opsForValue()
        .set(
            REFRESH_PREFIX + user.getLoginId(),
            refreshToken,
            Duration.ofMillis(refreshTokenExpiration));

    // 쿠키로 Refresh Token 전달
    addRefreshTokenToCookie(response, refreshToken);

    return LoginResponse.from(user.getId(), accessToken, user.getRole());
  }

  @Transactional
  public LoginResponse reissueAccessToken(HttpServletRequest request) {
    String refreshToken = extractTokenFromCookie(request);
    if (refreshToken == null) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_MISSING);
    }

    if (!jwtUtil.validateRefreshToken(refreshToken)) {
      throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    String loginId = jwtUtil.extractLoginId(refreshToken);

    String stored = redisTemplate.opsForValue().get(REFRESH_PREFIX + loginId);
    if (stored == null || !stored.equals(refreshToken)) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_MISSING);
    }

    User user =
        userRepository
            .findByLoginId(loginId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    String newAccessToken = jwtTokenProvider.generateAccessToken(user);

    return LoginResponse.from(user.getId(), newAccessToken, user.getRole());
  }

  @Transactional
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = extractTokenFromCookie(request);

    // refreshToken이 없으면 유효하지 않은 상태이므로 쿠키만 만료시키고 종료
    if (refreshToken == null) {
      expireRefreshTokenCookie(response);
      return;
    }

    // 토큰 유효성 검증 실패 시에도 쿠키는 만료시키고 종료
    if (!jwtUtil.validateRefreshToken(refreshToken)) {
      expireRefreshTokenCookie(response);
      return;
    }

    String loginId = jwtUtil.extractLoginId(refreshToken);

    // Redis에서 refresh 토큰 삭제
    redisTemplate.delete(REFRESH_PREFIX + loginId);

    // 쿠키 만료
    expireRefreshTokenCookie(response);
  }

  private void addRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
    String cookie =
        String.format(
            "refreshToken=%s; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=%d; Domain=%s",
            refreshToken, refreshTokenExpiration / 1000, frontendDomain);
    response.addHeader("Set-Cookie", cookie);
  }

  private String extractTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies()) {
      if ("refreshToken".equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private void expireRefreshTokenCookie(HttpServletResponse response) {
    String expiredCookie =
        String.format(
            "refreshToken=; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=0; Domain=%s",
            frontendDomain);
    response.addHeader("Set-Cookie", expiredCookie);
  }
}
