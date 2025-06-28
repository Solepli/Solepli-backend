package com.ilta.solepli.domain.profile.dto.response;

import lombok.Builder;

@Builder
public record UserProfileResponse(String profileImageUrl, String nickname, String loginType) {}
