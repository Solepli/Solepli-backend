package com.ilta.solepli.domain.profile.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.profile.dto.response.UserProfileResponse;
import com.ilta.solepli.domain.profile.service.ProfileService;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.response.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
@Tag(name = "ProfileController", description = "프로필 관련 API")
public class ProfileController {

  private final ProfileService profileService;

  @Operation(summary = "프로필 조회 API", description = "프로필을 조회하는 API입니다.")
  @PostMapping
  public ResponseEntity<SuccessResponse<UserProfileResponse>> createSollect(
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    UserProfileResponse response = profileService.getProfile(userDetails.user());

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }

  @Operation(
      summary = "닉네임 검증 API",
      description = "닉네임을 검증하는 API입니다. 중복여부, 입력 불가능한 닉네임 포함 여부를 검증합니다.")
  @GetMapping("/validate/nickname")
  public ResponseEntity<SuccessResponse<Boolean>> validateNickname(@NotNull String nickname) {
    Boolean isValidated = profileService.validateNickname(nickname);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(isValidated));
  }
}
