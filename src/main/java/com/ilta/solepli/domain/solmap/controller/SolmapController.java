package com.ilta.solepli.domain.solmap.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.solmap.dto.*;
import com.ilta.solepli.domain.solmap.service.SolmapService;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.response.SuccessResponse;

@RestController
@RequestMapping("/api/solmap")
@RequiredArgsConstructor
@Tag(name = "SolmapController", description = "쏠맵 관련 API")
public class SolmapController {

  private final SolmapService solmapService;

  @Operation(summary = "지도 화면 내 장소 마커 정보 조회 API", description = "지도 화면 내 장소 마커 정보들을 조회하는 API 입니다.")
  @GetMapping("/markers")
  public ResponseEntity<SuccessResponse<List<MarkerResponse>>> getMarkersByViewport(
      @RequestParam Double swLat,
      @RequestParam Double swLng,
      @RequestParam Double neLat,
      @RequestParam Double neLng,
      @RequestParam(required = false) String category,
      @AuthenticationPrincipal CustomUserDetails customUserDetails) {

    return ResponseEntity.ok()
        .body(
            SuccessResponse.successWithData(
                solmapService.getMarkersByViewport(
                    swLat, swLng, neLat, neLng, category, customUserDetails)));
  }

  @Operation(summary = "최근 검색어 저장 API", description = "최근 검색어를 저장하는 API 입니다.")
  @PostMapping("/search/recent")
  public ResponseEntity<SuccessResponse<Void>> addRecentSearch(
      @AuthenticationPrincipal CustomUserDetails customUserDetails,
      @Valid @RequestBody KeywordRequest keywordRequest) {

    solmapService.addRecentSearch(customUserDetails.getUsername(), keywordRequest.getKeyword());

    return ResponseEntity.status(201)
        .body(
            SuccessResponse.successWithNoData(
                keywordRequest.getKeyword() + " 검색어가 최근 목록에 반영 되었습니다."));
  }

  @Operation(summary = "최근 검색어 조회 API", description = "최근 검색어를 조회하는 API 입니다.")
  @GetMapping("/search/recent")
  public ResponseEntity<SuccessResponse<List<String>>> getRecentSearch(
      @AuthenticationPrincipal CustomUserDetails customUserDetails) {

    List<String> recentSearch = solmapService.getRecentSearch(customUserDetails.getUsername());

    return ResponseEntity.ok(SuccessResponse.successWithData(recentSearch));
  }

  @Operation(summary = "최근 검색어 삭제 API", description = "최근 검색어를 삭제하는 API 입니다.")
  @DeleteMapping("/search/recent/{keyword}")
  public ResponseEntity<SuccessResponse<Void>> deleteRecentSearch(
      @AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable String keyword) {

    solmapService.deleteRecentSearch(customUserDetails.getUsername(), keyword);

    return ResponseEntity.ok(SuccessResponse.successWithNoData(keyword + " 검색어 삭제 성공"));
  }

  @Operation(
      summary = "지도 화면 내 선택된 카테고리별 장소 리스트 조회 API",
      description = "지도 화면 내 선택된 카테고리별 장소 리스트 조회 API 입니다.")
  @GetMapping("/places")
  public ResponseEntity<SuccessResponse<PlaceSearchPreviewResponse>> getPlacesPreview(
      @AuthenticationPrincipal CustomUserDetails customUserDetails,
      @RequestParam Double swLat,
      @RequestParam Double swLng,
      @RequestParam Double neLat,
      @RequestParam Double neLng,
      @RequestParam Double userLat,
      @RequestParam Double userLng,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(required = false) Double cursorDist,
      @RequestParam(required = false, defaultValue = "5") int limit) {

    PlaceSearchPreviewResponse response =
        solmapService.getPlacesPreview(
            swLat,
            swLng,
            neLat,
            neLng,
            userLat,
            userLng,
            category,
            cursorId,
            cursorDist,
            limit,
            customUserDetails);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }

  @Operation(summary = "연관 검색어 조회 API", description = "연관 검색어 조회 API 입니다.")
  @GetMapping("search/related")
  public ResponseEntity<SuccessResponse<List<RelatedSearchResponse>>> getRelatedSearch(
      @RequestParam String keyword,
      @RequestParam(required = false) Double userLat,
      @RequestParam(required = false) Double userLng,
      @AuthenticationPrincipal CustomUserDetails customUserDetails) {

    List<RelatedSearchResponse> response =
        solmapService.getRelatedSearch(keyword, userLat, userLng, customUserDetails);

    return ResponseEntity.ok(SuccessResponse.successWithData(response));
  }

  @Operation(summary = "지역 검색 마커 정보 조회 API", description = "지역 검색 마커 정보 조회 API 입니다.")
  @GetMapping("/region/{regionName}/markers")
  public ResponseEntity<SuccessResponse<List<MarkerResponse>>> getMarkersByRegion(
      @PathVariable String regionName,
      @AuthenticationPrincipal CustomUserDetails customUserDetails) {

    List<MarkerResponse> response = solmapService.getMarkersByRegion(regionName, customUserDetails);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }

  @Operation(summary = "지역 검색 장소 리스트 조회 API", description = "지역 검색 장소 리스트 조회 API 입니다.")
  @GetMapping("/region/{regionName}/places")
  public ResponseEntity<SuccessResponse<PlaceSearchPreviewResponse>> getPlacesByRegionPreview(
      @AuthenticationPrincipal CustomUserDetails customUserDetails,
      @PathVariable String regionName,
      @RequestParam Double userLat,
      @RequestParam Double userLng,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(required = false) Double cursorDist,
      @RequestParam(required = false, defaultValue = "5") int limit) {

    PlaceSearchPreviewResponse response =
        solmapService.getPlacesByRegionPreview(
            regionName, userLat, userLng, category, cursorId, cursorDist, limit, customUserDetails);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }

  @Operation(summary = "장소 상세정보 조회 API", description = "장소 상세정보 조회 API 입니다.")
  @GetMapping("/place/search/{id}")
  public ResponseEntity<SuccessResponse<PlaceDetailSearchResponse>> getPlaceDetail(
      @AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long id) {

    PlaceDetailSearchResponse response = solmapService.getPlaceDetail(customUserDetails, id);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }

  @Operation(summary = "연관 검색어 결과 장소 마커 정보 조회 API", description = "연관 검색어 결과 장소 마커 정보 조회 API 입니다.")
  @GetMapping("/markers/search/related")
  public ResponseEntity<SuccessResponse<List<MarkerResponse>>> getMarkersByRelatedSearch(
      @RequestParam List<Long> ids, @AuthenticationPrincipal CustomUserDetails customUserDetails) {

    List<MarkerResponse> response = solmapService.getMarkersByRelatedSearch(ids, customUserDetails);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }

  @Operation(summary = "연관 검색어 결과 장소 리스트 조회 API", description = "연관 검색어 결과 장소 리스트 조회 API 입니다.")
  @GetMapping("/places/search/related")
  public ResponseEntity<SuccessResponse<PlaceSearchPreviewResponse>> getPlacePreviewByRelatedSearch(
      @AuthenticationPrincipal CustomUserDetails customUserDetails,
      @RequestParam List<Long> ids,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(required = false, defaultValue = "5") int limit) {

    PlaceSearchPreviewResponse response =
        solmapService.getPlacePreviewByRelatedSearch(ids, cursorId, limit, customUserDetails);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }

  @Operation(summary = "검색결과 없을시 주변 장소 조회 API", description = "검색결과 없을시 주변 장소 조회 API 입니다.")
  @GetMapping("/places/nearby")
  public ResponseEntity<SuccessResponse<PlaceSearchPreviewResponse>> getPlacesPreviewNearby(
      @AuthenticationPrincipal CustomUserDetails customUserDetails,
      @RequestParam Double userLat,
      @RequestParam Double userLng,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(required = false) Double cursorDist,
      @RequestParam(required = false, defaultValue = "5") int limit) {

    PlaceSearchPreviewResponse response =
        solmapService.getPlacesPreviewNearby(
            userLat, userLng, cursorId, cursorDist, limit, customUserDetails);

    return ResponseEntity.ok().body(SuccessResponse.successWithData(response));
  }
}
