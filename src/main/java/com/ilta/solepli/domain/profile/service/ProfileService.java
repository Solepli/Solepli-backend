package com.ilta.solepli.domain.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.profile.dto.response.UserProfileResponse;
import com.ilta.solepli.domain.profile.validator.NicknameValidator;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.repository.UserRepository;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;

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

  @Transactional(readOnly = true)
  public Boolean validateNickname(String nickname) {

    if (!NicknameValidator.isLengthValid(nickname)) {
      throw new CustomException(ErrorCode.EXCEEDS_LENGTH_NICKNAME);
    }

    if (!NicknameValidator.isValidFormat(nickname)) {
      throw new CustomException(ErrorCode.INVALID_FORMAT_NICKNAME);
    }

    if (userRepository.existsByNickname((nickname))) {
      throw new CustomException(ErrorCode.DUPLICATED_NICKNAME);
    }

    return true;
  }
}
