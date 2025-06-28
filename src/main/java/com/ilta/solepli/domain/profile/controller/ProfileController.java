package com.ilta.solepli.domain.profile.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.profile.dto.request.UserProfilePatchRequest;
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
  public ResponseEntity<SuccessResponse<Boolean>> validateNickname(
      @RequestParam(required = true) String nickname) {
    Boolean isValidated = profileService.validateNickname(nickname);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(isValidated));
  }

  @Operation(summary = "프로필 수정 API", description = "프로필을 수정하는 API입니다.")
  @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<Void>> patchProfile(
      @Parameter(
              description = "프로필 수정 데이터 (JSON)",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = UserProfilePatchRequest.class)))
          @RequestPart(name = "request", required = false)
          UserProfilePatchRequest request,
      @Parameter(
              name = "file",
              description = "프로필 이미지",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                      schema = @Schema(type = "string", format = "binary")))
          @RequestPart(name = "files", required = false)
          MultipartFile file,
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    profileService.patchProfile(userDetails.user(), request, file);

    return ResponseEntity.ok().body(SuccessResponse.successWithNoData("프로필 수정 성공"));
  }
}
