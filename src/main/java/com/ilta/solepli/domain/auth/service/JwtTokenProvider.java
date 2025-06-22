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
  private final long expiration;

  public JwtTokenProvider(
      @Value("${spring.jwt.secret}") String secretKey,
      @Value("${spring.jwt.access.token.expiration}") long expiration) {
    this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    this.expiration = expiration;
  }

  public String generateToken(User user) {
    // 토큰 생성 (loginId + role 포함)
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .setSubject(user.getLoginId())
        .claim("role", user.getRole().name())
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }
}
