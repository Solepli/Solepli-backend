package com.ilta.solepli.domain.place.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.place.dto.reqeust.AddPlaceRequest;
import com.ilta.solepli.domain.place.dto.reqeust.RequestAddPlaceRequest;
import com.ilta.solepli.domain.place.dto.response.PlaceSearchResponse;
import com.ilta.solepli.domain.place.service.PlaceService;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.response.SuccessResponse;
import com.ilta.solepli.global.util.SecurityUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place")
@Tag(name = "PlaceController", description = "장소 관련 API")
public class PlaceController {

  private final PlaceService placeService;

  @Operation(summary = "장소 검색 API", description = "장소 추가 화면에서 사용되는 검색 API입니다.")
  @GetMapping("/search")
  public ResponseEntity<SuccessResponse<PlaceSearchResponse>> searchPlaces(
      @AuthenticationPrincipal CustomUserDetails customUserDetails,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = true) String keyword) {

    User user = SecurityUtil.getUser(customUserDetails);
    PlaceSearchResponse searchContents =
        placeService.getSearchPlaces(user, cursorId, size, keyword);

    return ResponseEntity.ok(SuccessResponse.successWithData(searchContents));
  }

  @Operation(summary = "장소 추가 요청 API", description = "사용자가 장소 추가 요청 하는 API 입니다.")
  @PostMapping("/request")
  public ResponseEntity<SuccessResponse<Void>> requestAddPlace(
      @AuthenticationPrincipal CustomUserDetails customUserDetails,
      @Valid @RequestBody RequestAddPlaceRequest requestAddPlaceRequest) {

    placeService.requestAddPlace(customUserDetails, requestAddPlaceRequest);

    return ResponseEntity.ok(SuccessResponse.successWithNoData("장소 추가 요청 성공"));
  }

  @Operation(summary = "장소 추가 API", description = "장소 추가 API 입니다.")
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<SuccessResponse<Void>> addPlace(
      @Valid @RequestBody AddPlaceRequest addPlaceRequest) {

    placeService.addPlace(addPlaceRequest);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.successWithNoData("장소 추가 성공"));
  }
}
