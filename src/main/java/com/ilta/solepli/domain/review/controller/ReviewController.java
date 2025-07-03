package com.ilta.solepli.domain.review.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.review.dto.request.ReviewCreateRequest;
import com.ilta.solepli.domain.review.dto.response.ReviewPageResponse;
import com.ilta.solepli.domain.review.service.ReviewService;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.response.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
@Tag(name = "ReviewController", description = "리뷰 관련 API")
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "리뷰 등록 API", description = "장소에 대한 리뷰를 등록하는 API입니다.")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SuccessResponse<Void>> createReview(
      @Parameter(
              description = "리뷰 작성 데이터 (JSON)",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ReviewCreateRequest.class)))
          @Valid
          @RequestPart
          ReviewCreateRequest request,
      @Parameter(
              name = "files",
              description = "리뷰 이미지 파일들 (최대 5장, JPG/PNG)",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                      schema = @Schema(type = "string", format = "binary")))
          @RequestPart(name = "files", required = false)
          List<MultipartFile> files,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    reviewService.createReview(request, files, userDetails.user());
    return ResponseEntity.ok().body(SuccessResponse.successWithNoData("리뷰 등록 성공"));
  }

  @Operation(summary = "리뷰 조회 API", description = "리뷰 조회 API 입니다.")
  @GetMapping("/place/{id}")
  public ResponseEntity<SuccessResponse<ReviewPageResponse>> getReviewDetails(
      @PathVariable Long id,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(required = false, defaultValue = "5") int limit) {

    ReviewPageResponse response = reviewService.getReviewDetails(id, cursorId, limit);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }
}
