package com.ilta.solepli.global.util;

import java.security.Key;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;

@Component
public class JwtUtil {

  private final Key key;

  public JwtUtil(@Value("${spring.jwt.secret}") String secretKey) {
    this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
  }

  public String getLoginIdFromToken(String token) {
    // 토큰에서 사용자 ID 추출
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;

    } catch (ExpiredJwtException e) {
      throw new CustomException(ErrorCode.TOKEN_EXPIRED);
    } catch (UnsupportedJwtException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN); // 지원하지 않는 형식
    } catch (MalformedJwtException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN); // 잘못된 구조
    } catch (io.jsonwebtoken.security.SignatureException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN); // 서명 오류
    } catch (IllegalArgumentException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN); // 널 or 빈 문자열
    } catch (JwtException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN); // 기타 JWT 에러
    }
  }
}
