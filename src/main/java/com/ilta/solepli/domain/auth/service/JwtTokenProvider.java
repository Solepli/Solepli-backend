package com.ilta.solepli.domain.auth.service;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import com.ilta.solepli.domain.user.entity.User;

@Component
public class JwtTokenProvider {

  private final Key key;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtTokenProvider(
      @Value("${spring.jwt.secret}") String secretKey,
      @Value("${spring.jwt.access.token.expiration}") long accessTokenExpiration,
      @Value("${spring.jwt.refresh.token.expiration}") long refreshTokenExpiration) {
    this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  /** Access Token 생성 (사용자 인증용, 짧은 유효기간) */
  public String generateAccessToken(User user) {
    // 토큰 생성 (loginId + role 포함)
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessTokenExpiration);

    return Jwts.builder()
        .setSubject(user.getLoginId())
        .claim("role", user.getRole().name())
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  /** Refresh Token 생성 (Access Token 재발급용, claim 없음) */
  public String generateRefreshToken(User user) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + refreshTokenExpiration);

    return Jwts.builder()
        .setSubject(user.getLoginId())
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }
}
