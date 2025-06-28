package com.ilta.solepli.domain.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.profile.dto.response.UserProfileResponse;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ProfileService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(User user) {
    UserProfileResponse response =
        UserProfileResponse.builder()
            .profileImageUrl(user.getProfileImageUrl())
            .nickname(user.getNickname())
            .loginType(user.getLoginType().toString())
            .build();

    return response;
  }
}
