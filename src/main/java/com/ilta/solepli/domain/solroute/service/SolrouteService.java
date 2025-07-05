package com.ilta.solepli.domain.solroute.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.place.entity.Place;
import com.ilta.solepli.domain.place.repository.PlaceRepository;
import com.ilta.solepli.domain.solmark.place.entity.SolmarkPlace;
import com.ilta.solepli.domain.solmark.place.repository.SolmarkPlaceRepository;
import com.ilta.solepli.domain.solroute.dto.PlaceWithReviewCountDto;
import com.ilta.solepli.domain.solroute.dto.request.SolrouteCreateRequest;
import com.ilta.solepli.domain.solroute.dto.request.SolrouteCreateRequest.PlaceInfo;
import com.ilta.solepli.domain.solroute.dto.request.SolroutePatchRequest;
import com.ilta.solepli.domain.solroute.dto.response.PlacePreviewResponse;
import com.ilta.solepli.domain.solroute.dto.response.PlaceSummaryResponse;
import com.ilta.solepli.domain.solroute.dto.response.SolrouteDetailResponse;
import com.ilta.solepli.domain.solroute.dto.response.SolroutePreviewResponse;
import com.ilta.solepli.domain.solroute.entity.Solroute;
import com.ilta.solepli.domain.solroute.entity.SolroutePlace;
import com.ilta.solepli.domain.solroute.repository.SolroutePlaceRepository;
import com.ilta.solepli.domain.solroute.repository.SolrouteRepository;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.util.PlaceUtil;

@Service
@RequiredArgsConstructor
public class SolrouteService {

  private final SolrouteRepository solrouteRepository;
  private final SolroutePlaceRepository solroutePlaceRepository;
  private final PlaceRepository placeRepository;
  private final SolmarkPlaceRepository solmarkPlaceRepository;

  @Transactional
  public void createSolroute(User user, SolrouteCreateRequest request) {

    Solroute solroute =
        Solroute.builder().iconId(request.iconId()).name(request.name()).user(user).build();

    List<PlaceInfo> placeInfos = request.placeInfos();
    for (PlaceInfo placeInfo : placeInfos) {
      Place place =
          placeRepository
              .findById(placeInfo.id())
              .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_EXISTS));

      SolroutePlace solroutePlace =
          SolroutePlace.builder().place(place).memo(placeInfo.memo()).seq(placeInfo.seq()).build();

      solroute.addSolroutePlace(solroutePlace); // 연관관계 & 리스트 추가
    }

    solrouteRepository.save(solroute); // cascade로 SolroutePlace까지 저장
  }

  @Transactional(readOnly = true)
  public List<PlaceSummaryResponse> findNearbyPopularPlace(User user, Long placeId) {
    Place place =
        placeRepository
            .findById(placeId)
            .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_EXISTS));

    double baseLat = place.getLatitude();
    double baseLng = place.getLongitude();

    double latDelta = 1.0 / 111.0;
    double lngDelta = 1.0 / (111.0 * Math.cos(Math.toRadians(baseLat)));

    double minLat = baseLat - latDelta;
    double maxLat = baseLat + latDelta;
    double minLng = baseLng - lngDelta;
    double maxLng = baseLng + lngDelta;

    // 1차 사각형 필터
    List<PlaceWithReviewCountDto> candidates =
        placeRepository.findPlacesWithReviewCountInArea(minLat, maxLat, minLng, maxLng);

    // 정확히 1KM 내에 있는 장소만 추출
    List<Place> result =
        candidates.stream()
            .filter(dto -> !dto.place().getId().equals(placeId))
            .filter(
                dto ->
                    PlaceUtil.calculateDistance(
                            baseLat, baseLng, dto.place().getLatitude(), dto.place().getLongitude())
                        <= 1000)
            .sorted(Comparator.comparingLong(PlaceWithReviewCountDto::reviewCount).reversed())
            .limit(2)
            .map(PlaceWithReviewCountDto::place)
            .toList();

    List<PlaceSummaryResponse> response = new ArrayList<>();
    Set<Long> markedSet = getSolmarkedPlaceIds(user, result);

    for (Place p : result) {
      List<String> tags = placeRepository.getTopTagsForPlace(p.getId(), 3);
      Integer recommendationPercent = placeRepository.getRecommendationPercent(p.getId());
      boolean isMarked = markedSet.contains(p.getId());

      PlaceSummaryResponse placeSummaryResponse =
          PlaceSummaryResponse.builder()
              .name(p.getName())
              .detailedCategory(p.getTypes())
              .recommendationPercent(recommendationPercent)
              .tags(tags)
              .isMarked(isMarked)
              .rating(PlaceUtil.truncateTo2Decimals(p.getRating()))
              .build();

      response.add(placeSummaryResponse);
    }

    return response;
  }

  @Transactional(readOnly = true)
  public PlacePreviewResponse getPlacePreview(Long placeId) {
    Place place =
        placeRepository
            .findByPlaceId(placeId)
            .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_EXISTS));

    return PlacePreviewResponse.builder()
        .placeId(place.getId())
        .placeName(place.getName())
        .detailedCategory(place.getTypes())
        .address(place.getAddress())
        .category(place.getPlaceCategories().get(0).getCategory().getName())
        .latitude(place.getLatitude())
        .longitude(place.getLongitude())
        .build();
  }

  @Transactional
  public Boolean updateSolrouteStatus(Long solrouteId, User user) {
    Solroute solroute = getSolrouteOrThrow(solrouteId, user);

    return solroute.updateStatus();
  }

  @Transactional(readOnly = true)
  public List<SolroutePreviewResponse> getSolroutePreviews(User user) {
    return solrouteRepository.findAllByUserId(user).stream()
        .map(SolroutePreviewResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public SolrouteDetailResponse getSolrouteDeatil(Long solrouteId, User user) {
    Solroute solroute = getSolrouteWithPlacesOrThrow(solrouteId, user);

    List<SolroutePlace> solroutePlaces =
        solroutePlaceRepository.findBySolrouteIdWithPlaceAndCategory(solroute.getId());

    List<SolrouteDetailResponse.PlaceInfo> placeInfos =
        solroutePlaces.stream().map(SolrouteDetailResponse.PlaceInfo::from).toList();

    return SolrouteDetailResponse.builder()
        .id(solroute.getId())
        .iconId(solroute.getIconId())
        .name(solroute.getName())
        .status(solroute.getStatus())
        .placeInfos(placeInfos)
        .build();
  }

  @Transactional
  public void patchSolroute(SolroutePatchRequest request, Long solrouteId, User user) {
    Solroute solroute = getSolrouteOrThrow(solrouteId, user);

    solroute.updateInfo(request.iconId(), request.name());

    List<SolroutePatchRequest.PlaceInfo> placeInfos = request.placeInfos();

    if (placeInfos != null) {
      // 기존 SolroutePlace들 삭제
      solroutePlaceRepository.deleteAllBySolrouteId(solroute);
      // 1차 캐시와 동기화
      solroute.getSolroutePlaces().clear();

      for (SolroutePatchRequest.PlaceInfo placeInfo : placeInfos) {
        Place place =
            placeRepository
                .findById(placeInfo.id())
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_EXISTS));

        SolroutePlace solroutePlace =
            SolroutePlace.builder()
                .place(place)
                .memo(placeInfo.memo())
                .seq(placeInfo.seq())
                .build();

        // 연관관계 & 리스트 추가
        solroute.addSolroutePlace(solroutePlace);
      }
    }
  }

  @Transactional
  public void deleteSolroute(Long solrouteId, User user) {
    Solroute solroute = getSolrouteOrThrow(solrouteId, user);

    solroute.softDelete();
  }

  private Set<Long> getSolmarkedPlaceIds(User user, List<Place> places) {
    // 비로그인이거나 조회된 장소가 없으면 빈 Set 반환
    if (user == null & places.isEmpty()) {
      return Collections.emptySet();
    }

    List<Long> placeIds = places.stream().map(Place::getId).toList();

    // 쏠마크 장소 조회
    List<SolmarkPlace> solmarkPlaces =
        solmarkPlaceRepository.findAllNonDeletedByUserAndPlaceIds(user, placeIds);

    // 쏠마크 장소 ID Set 반환
    return solmarkPlaces.stream().map(sp -> sp.getPlace().getId()).collect(Collectors.toSet());
  }

  private Solroute getSolrouteOrThrow(Long solrouteId, User user) {
    return solrouteRepository
        .findByIdAndUser(solrouteId, user)
        .orElseThrow(() -> new CustomException(ErrorCode.SOLROUTE_ACCESS_DENIED));
  }

  private Solroute getSolrouteWithPlacesOrThrow(Long solrouteId, User user) {
    return solrouteRepository
        .findByIdAndUserWithPlaces(solrouteId, user)
        .orElseThrow(() -> new CustomException(ErrorCode.SOLROUTE_ACCESS_DENIED));
  }
}
