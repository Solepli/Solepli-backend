package com.ilta.solepli.domain.profile.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.profile.dto.request.UserProfilePatchRequest;
import com.ilta.solepli.domain.profile.dto.response.UserProfileResponse;
import com.ilta.solepli.domain.profile.validator.NicknameValidator;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.repository.UserRepository;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.service.S3Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

  private final UserRepository userRepository;
  private final S3Service s3Service;

  @Value("${DEFAULT_PROFILE_URL}")
  private String defaultImageUrl;

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

    if (userRepository.existsByNickname((nickname))) {
      throw new CustomException(ErrorCode.DUPLICATED_NICKNAME);
    }

    if (!NicknameValidator.isValidFormat(nickname)) {
      throw new CustomException(ErrorCode.INVALID_FORMAT_NICKNAME);
    }

    return true;
  }

  @Transactional
  public void patchProfile(User user, UserProfilePatchRequest request, MultipartFile file) {

    // 매개변수 user는 Detached 객체여서 반영이 안됌
    User findUser =
        userRepository
            .findById(user.getId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    if (request != null) {
      findUser.patchNickname(request.nickname());
    }

    if (file != null) {
      if (!findUser.getProfileImageUrl().equals(defaultImageUrl)) {
        s3Service.deleteProfileImage(findUser.getProfileImageUrl());
      }

      String profileImageUrl = s3Service.uploadProfileImage(file);
      findUser.patchProfileImageUrl(profileImageUrl);
    }
  }
}
