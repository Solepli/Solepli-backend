package com.ilta.solepli.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.user.service.UserService;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.response.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = "UserController", description = "계정 관련 API")
public class UserController {

  private final UserService userService;

  @Operation(summary = "계정 삭제 API", description = "계정 삭제 API 입니다.")
  @DeleteMapping
  public ResponseEntity<SuccessResponse<Void>> addRecentSearch(
      @AuthenticationPrincipal CustomUserDetails customUserDetails) {

    userService.deleteUser(customUserDetails.user());

    return ResponseEntity.ok().body(SuccessResponse.successWithNoData("계정 삭제가 완료 되었습니다."));
  }
}
