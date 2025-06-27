package com.ilta.solepli.domain.auth.dto.response;

import lombok.Builder;

import com.ilta.solepli.domain.user.entity.Role;

@Builder
public record LoginResponse(Long userId, String accessToken, Role role) {
  public static LoginResponse from(Long userId, String accessToken, Role role) {
    return LoginResponse.builder().userId(userId).accessToken(accessToken).role(role).build();
  }
}
