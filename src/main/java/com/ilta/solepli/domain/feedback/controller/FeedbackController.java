package com.ilta.solepli.domain.feedback.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.feedback.dto.AddFeedbackRequest;
import com.ilta.solepli.domain.feedback.service.FeedbackService;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.response.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedback")
@Tag(name = "FeedbackController", description = "의견 관련 API")
public class FeedbackController {

  private final FeedbackService feedbackService;

  @Operation(summary = "의견 남기기 API", description = "작성한 의견을 쏠플리 메일로 전송하는 API 입니다.")
  @PostMapping("")
  public ResponseEntity<SuccessResponse<Void>> sendFeedbackEmail(
      @AuthenticationPrincipal CustomUserDetails customUserDetails,
      @Valid @RequestBody AddFeedbackRequest addFeedbackRequest)
      throws MessagingException {

    feedbackService.sendFeedbackEmail(customUserDetails, addFeedbackRequest);

    return ResponseEntity.ok().body(SuccessResponse.successWithNoData("의견 메일 전송 성공"));
  }
}
