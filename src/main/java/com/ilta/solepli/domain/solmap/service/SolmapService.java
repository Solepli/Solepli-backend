package com.ilta.solepli.domain.solmap.service;

import static com.ilta.solepli.global.util.OpenStatusUtil.getOpenStatus;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.ilta.solepli.domain.category.entity.QCategory;
import com.ilta.solepli.domain.category.repository.CategoryRepository;
import com.ilta.solepli.domain.place.entity.Place;
import com.ilta.solepli.domain.place.entity.PlaceCategory;
import com.ilta.solepli.domain.place.entity.QPlace;
import com.ilta.solepli.domain.place.entity.QPlaceCategory;
import com.ilta.solepli.domain.place.entity.QPlaceHour;
import com.ilta.solepli.domain.place.repository.PlaceRepository;
import com.ilta.solepli.domain.review.dto.response.ReviewDetail;
import com.ilta.solepli.domain.review.entity.QReview;
import com.ilta.solepli.domain.review.entity.Review;
import com.ilta.solepli.domain.review.entity.mapping.ReviewImage;
import com.ilta.solepli.domain.review.entity.mapping.ReviewTag;
import com.ilta.solepli.domain.review.repository.ReviewRepository;
import com.ilta.solepli.domain.review.repository.ReviewTagCustomRepository;
import com.ilta.solepli.domain.solmap.dto.*;
import com.ilta.solepli.domain.solmap.entity.SearchType;
import com.ilta.solepli.domain.solmark.place.entity.SolmarkPlace;
import com.ilta.solepli.domain.solmark.place.repository.SolmarkPlaceRepository;
import com.ilta.solepli.domain.tag.entity.TagType;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.dto.OpenStatus;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.util.SecurityUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolmapService {

  private final PlaceRepository placeRepository;
  private final CategoryRepository categoryRepository;
  private final ReviewTagCustomRepository reviewTagCustomRepository;
  private final ReviewRepository reviewRepository;
  private final SolmarkPlaceRepository solmarkPlaceRepository;

  private final RedisTemplate<String, Object> redisTemplate;
  private final JPAQueryFactory jpaQueryFactory;
  private final QPlace p = QPlace.place;
  private final QPlaceCategory pc = QPlaceCategory.placeCategory;
  private final QCategory c = QCategory.category;
  private final QPlaceHour ph = QPlaceHour.placeHour;
  private final QReview r = QReview.review;

  private static final String RECENT_SEARCH_PREFIX = "solmap_recent_search:";
  private static final int MAX_RECENT_SEARCH = 10;
  private static final int TAG_LIMIT = 3;
  private static final int PREVIEW_THUMBNAIL_LIMIT = 3;
  private static final int MAX_RELATED_SEARCH = 10;
  private static final int MAX_PLACE_THUMBNAIL_LIMIT = 5;
  private static final int INITIAL_REVIEW_LIMIT = 5;
  private static final int NEARBY_RADIUS_KM_LIMIT = 2;

  @Transactional(readOnly = true)
  public List<MarkerResponse> getMarkersByViewport(
      Double swLat,
      Double swLng,
      Double neLat,
      Double neLng,
      String category,
      CustomUserDetails customUserDetails) {

    // 좌표, 카테고리 유효성 검증
    validViewport(swLat, swLng, neLat, neLng);
    validCategory(category);

    // 사용자 로그인, 비로그인 확인
    User user = SecurityUtil.getUser(customUserDetails);

    // 좌표에 속한 장소 조회
    List<Place> places =
        placeRepository.findInViewportWithOptionalCategory(swLat, swLng, neLat, neLng, category);

    // 쏠마크한 PlaceId 리스트 조회
    Set<Long> solmarkedPlaceIds = getSolmarkedPlaceIds(user, places);

    // 마커 관련 데이터 리스트
    return places.stream().map(p -> toMarkerDetail(p, category, solmarkedPlaceIds)).toList();
  }

  private void validViewport(Double swLat, Double swLng, Double neLat, Double neLng) {
    if (swLat > neLat || swLng > neLng) {
      throw new CustomException(ErrorCode.INVALID_VIEWPORT_COORDINATES);
    }
  }

  private void validCategory(String category) {
    if (category != null && !categoryRepository.existsByName(category)) {
      throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
    }
  }

  private MarkerResponse toMarkerDetail(
      Place p, String selectedCategory, Set<Long> solmarkedPlaceIds) {
    String category;

    if (selectedCategory != null) {
      category = selectedCategory;
    } else {
      category =
          p.getPlaceCategories().stream()
              .sorted(Comparator.comparing(PlaceCategory::getId))
              .map(pc -> pc.getCategory().getName())
              .findFirst()
              .orElseThrow(() -> new CustomException(ErrorCode.UNCATEGORIZED));
    }

    boolean isMarked = solmarkedPlaceIds.contains(p.getId());

    return MarkerResponse.builder()
        .id(p.getId())
        .latitude(p.getLatitude())
        .longitude(p.getLongitude())
        .category(category)
        .isMarked(isMarked)
        .build();
  }

  /**
   * 사용자가 검색한 키워드를 ZSET에 추가하고, 최대 저장 개수를 초과한 오래된 항목은 삭제한다.
   *
   * @param userId 사용자 식별자
   * @param keyword 검색어
   */
  public void addRecentSearch(String userId, String keyword) {
    String key = keyBuild(userId);
    long score = System.currentTimeMillis();

    // ZSET에 (키워드, timestamp) 쌍으로 추가
    redisTemplate.opsForZSet().add(key, keyword, score);

    // 최신 MAX_RECENT_SEARCH개를 제외한 나머지(가장 오래된) 삭제
    redisTemplate.opsForZSet().removeRange(key, 0, -MAX_RECENT_SEARCH - 1);
  }

  /**
   * Redis에 저장할 키를 생성한다.
   *
   * @param userId 사용자 식별자
   * @return "recent_search:{userId}" 형태의 최종 키
   */
  private String keyBuild(String userId) {
    return RECENT_SEARCH_PREFIX + userId;
  }

  /**
   * 사용자의 최근 검색어를 최신순으로 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 최대 MAX_RECENT_SEARCH개까지의 검색어 리스트
   */
  public List<String> getRecentSearch(String userId) {
    String key = keyBuild(userId);

    // ZSET을 score 내림차순으로 조회
    Set<Object> range = redisTemplate.opsForZSet().reverseRange(key, 0, MAX_RECENT_SEARCH - 1);

    if (range == null || range.isEmpty()) {
      return Collections.emptyList();
    }

    // Object → String 변환
    return range.stream().map(Object::toString).toList();
  }

  /**
   * 사용자의 특정 검색어를 ZSET에서 제거한다. 존재하지 않는 키워드면 404 예외를 던진다.
   *
   * @param userId 사용자 식별자
   * @param keyword 삭제할 검색어
   */
  public void deleteRecentSearch(String userId, String keyword) {
    String key = keyBuild(userId);

    Long removed = redisTemplate.opsForZSet().remove(key, keyword);

    // 삭제결과가 없거나 null 이면 예외 발생
    if (removed == null || removed == 0) {
      throw new CustomException(ErrorCode.RECENT_SEARCH_NOT_FOUND);
    }
  }

  @Transactional(readOnly = true)
  public PlaceSearchPreviewResponse getPlacesPreview(
      Double swLat,
      Double swLng,
      Double neLat,
      Double neLng,
      Double userLat,
      Double userLng,
      String category,
      Long cursorId,
      Double cursorDist,
      int limit,
      CustomUserDetails customUserDetails) {

    // 좌표, 카테고리 유효성 검증
    validViewport(swLat, swLng, neLat, neLng);
    validCategory(category);

    // 좌표에 속한 장소 조회
    List<Place> places =
        getPlacesByViewPort(
            swLat, swLng, neLat, neLng, userLat, userLng, category, cursorId, cursorDist, limit);

    // 사용자가 쏠마크한 장소 ID Set 추출
    User user = SecurityUtil.getUser(customUserDetails);
    Set<Long> solmarkedPlaceIds = getSolmarkedPlaceIds(user, places);

    // 다음 페이지 커서 값 세팅 (limit+1번째 데이터가 존재할 경우에만)
    CursorInfo next = setNextCursor(places, userLat, userLng, limit);

    // PreviewDetail DTO 매핑
    List<PlacePreviewDetail> placePreviewDetails =
        mapToPreviewDetails(places, limit, solmarkedPlaceIds);

    return PlaceSearchPreviewResponse.builder()
        .places(placePreviewDetails)
        .nextCursor(next.id())
        .nextCursorDist(next.distance())
        .build();
  }

  // 지도 뷰포트 내 포함 여부 조건
  private BooleanExpression viewPortIn(Double swLat, Double swLng, Double neLat, Double neLng) {
    return p.latitude.between(swLat, neLat).and(p.longitude.between(swLng, neLng));
  }

  private BooleanExpression categoryIn(String category) {
    if (category == null) {
      return null;
    }
    return c.name.eq(category);
  }

  // 장소 ~ 사용자 거리 계산(Haversine 공식, km단위)
  private NumberExpression<Double> distance(double userLat, double userLng) {
    return Expressions.numberTemplate(
        Double.class,
        "6371 * acos("
            + " cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) +"
            + " sin(radians({0})) * sin(radians({1}))"
            + ")",
        userLat, // {0}
        p.latitude, // {1}
        p.longitude, // {2}
        userLng // {3}
        );
  }

  private List<Place> getPlacesByViewPort(
      Double swLat,
      Double swLng,
      Double neLat,
      Double neLng,
      Double userLat,
      Double userLng,
      String category,
      Long cursorId,
      Double cursorDist,
      int limit) {
    NumberExpression<Double> distance = distance(userLat, userLng);

    return jpaQueryFactory
        .selectFrom(p)
        .distinct()
        .leftJoin(p.placeCategories, pc)
        .leftJoin(pc.category, c)
        .leftJoin(p.placeHours, ph)
        .where(
            viewPortIn(swLat, swLng, neLat, neLng),
            categoryIn(category),
            cursorAfter(cursorId, cursorDist, distance))
        .orderBy(distance.asc(), p.id.asc())
        .limit(limit + 1) // 커서 페이징을 위해 limit+1개 조회 (limit개 + nextCursor용 1개)
        .fetch();
  }

  // 커서(다음 페이지)용 거리 계산
  private Double getNextCursorDistance(
      Double userLat, Double userLng, Double placeLat, Double PlaceLng) {
    double radUserLat = Math.toRadians(userLat);
    double radUserLng = Math.toRadians(userLng);
    double radPlaceLat = Math.toRadians(placeLat);
    double radPlaceLng = Math.toRadians(PlaceLng);

    return 6371
        * Math.acos(
            Math.sin(radUserLat) * Math.sin(radPlaceLat)
                + Math.cos(radUserLat)
                    * Math.cos(radPlaceLat)
                    * Math.cos(radUserLng - radPlaceLng));
  }

  // 커서 이후 데이터 조건 (거리, id순)
  private BooleanExpression cursorAfter(
      Long cursorId, Double cursorDist, NumberExpression<Double> distance) {
    if (cursorId == null || cursorDist == null) {
      return null;
    }

    return distance.gt(cursorDist).or(distance.eq(cursorDist).and(p.id.gt(cursorId)));
  }

  @Transactional(readOnly = true)
  public List<RelatedSearchResponse> getRelatedSearch(
      String keyword, Double userLat, Double userLng, CustomUserDetails customUserDetails) {
    // 로그인, 비로그인 판별
    User user = SecurityUtil.getUser(customUserDetails);
    // 구 검색 결과 스트림
    Stream<RelatedSearchResponse> placesDistrictLike = getDistrictsByKeyword(keyword);
    // 동 검색 결과 스트림
    Stream<RelatedSearchResponse> placesNeighborhoodLike = getNeighborhoodsByKeyword(keyword);
    // 장소 검색 결과 스트림 (거리순)
    Stream<RelatedSearchResponse> placesNameLike =
        getPlacesByKeyword(keyword, userLat, userLng, user);

    // 스트림을 합쳐서, 앞에서부터 MAX개만 리스트로 수집
    return Stream.of(placesDistrictLike, placesNeighborhoodLike, placesNameLike)
        .flatMap(Function.identity())
        .limit(MAX_RELATED_SEARCH)
        .toList();
  }

  /** keyword가 포함된 구 명을 중복 없이 조회하여 DTO로 매핑한 스트림을 반환. */
  private Stream<RelatedSearchResponse> getDistrictsByKeyword(String keyword) {
    return jpaQueryFactory
        .select(p.district)
        .distinct()
        .from(p)
        .where(p.district.contains(keyword))
        .fetch()
        .stream()
        .map(s -> RelatedSearchResponse.builder().type(SearchType.DISTRICT).name(s).build());
  }

  /** keyword가 포함된 동 명을 중복 없이 조회하여 DTO로 매핑한 스트림을 반환. */
  private Stream<RelatedSearchResponse> getNeighborhoodsByKeyword(String keyword) {
    return jpaQueryFactory
        .select(p.neighborhood)
        .distinct()
        .from(p)
        .where(p.neighborhood.contains(keyword))
        .fetch()
        .stream()
        .map(s -> RelatedSearchResponse.builder().type(SearchType.DISTRICT).name(s).build());
  }

  /** keyword가 포함된 장소를 거리순으로 조회하여 DTO로 매핑한 스트림을 반환. */
  private Stream<RelatedSearchResponse> getPlacesByKeyword(
      String keyword, Double userLat, Double userLng, User user) {
    // 장소 조회
    List<Place> places = getPlaces(keyword, userLat, userLng);
    // 쏠마크한 PlaceId 리스트 조회
    Set<Long> solmarkedPlaceIds = getSolmarkedPlaceIds(user, places);

    return places.stream()
        .map(
            p -> {
              // 각 장소 쏠마크 여부 판별
              boolean isMarked = solmarkedPlaceIds.contains(p.getId());

              return RelatedSearchResponse.builder()
                  .id(p.getId())
                  .type(SearchType.PLACE)
                  .name(p.getName())
                  .address(p.getAddress())
                  // 미터 단위 계산 후, m/km DTO로 변환
                  .distance(
                      Distance.fromMeter(
                          (int)
                              calculateDistance(
                                  userLat, userLng, p.getLatitude(), p.getLongitude())))
                  .category(getMainCategory(p))
                  .isMarked(isMarked)
                  .build();
            });
  }

  private Set<Long> getSolmarkedPlaceIds(User user, List<Place> places) {
    // 비로그인이거나 조회된 장소가 없으면 빈 Set 반환
    if (user == null | places.isEmpty()) {
      return Collections.emptySet();
    }

    List<Long> placeIds = places.stream().map(Place::getId).toList();

    // 쏠마크 장소 조회
    List<SolmarkPlace> solmarkPlaces =
        solmarkPlaceRepository.findAllNonDeletedByUserAndPlaceIds(user, placeIds);

    // 쏠마크 장소 ID Set 반환
    return solmarkPlaces.stream().map(sp -> sp.getPlace().getId()).collect(Collectors.toSet());
  }

  private List<Place> getPlaces(String keyword, Double userLat, Double userLng) {
    return jpaQueryFactory
        .select(p)
        .from(p)
        .join(p.placeCategories, pc)
        .fetchJoin()
        .join(pc.category, c)
        .fetchJoin()
        .where(p.name.contains(keyword))
        .orderBy(distance(userLat, userLng).asc())
        .fetch();
  }

  /** 장소에 연결된 카테고리 중 첫 번째(대표) 카테고리명을 반환. */
  private String getMainCategory(Place place) {
    return place.getPlaceCategories().get(0).getCategory().getName();
  }

  /** 두 위경도 좌표 간의 거리를 계산하여 미터 단위(double)로 반환. */
  private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
    final int EARTH_RADIUS = 6371000; // 지구 반지름 (미터 단위)

    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);

    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS * c; // 결과: 미터(m) 단위 거리
  }

  public List<MarkerResponse> getMarkersByRegion(
      String regionName, CustomUserDetails customUserDetails) {

    User user = SecurityUtil.getUser(customUserDetails);
    // regionName과 동일한 구, 동 장소 조회
    return getMarkerByRegion(regionName, user);
  }

  private List<MarkerResponse> getMarkerByRegion(String regionName, User user) {
    List<Place> places = placeRepository.findAllByRegionName(regionName);
    // 쏠마크한 PlaceId 리스트 조회
    Set<Long> solmarkedPlaceIds = getSolmarkedPlaceIds(user, places);

    // 조회된 각 장소를 DTO 변환
    return places.stream().map(p -> getMarkerResponse(p, solmarkedPlaceIds)).toList();
  }

  private MarkerResponse getMarkerResponse(Place p, Set<Long> solmarkedPlaceIds) {
    boolean isMarked = solmarkedPlaceIds.contains(p.getId());
    // 응답 DTO 변환
    return MarkerResponse.builder()
        .id(p.getId())
        .latitude(p.getLatitude())
        .longitude(p.getLongitude())
        .category(getMainCategory(p))
        .isMarked(isMarked)
        .build();
  }

  @Transactional(readOnly = true)
  public PlaceSearchPreviewResponse getPlacesByRegionPreview(
      String regionName,
      Double userLat,
      Double userLng,
      String category,
      Long cursorId,
      Double cursorDist,
      int limit,
      CustomUserDetails customUserDetails) {

    // 카테고리, 지역명 유효성 검증
    validCategory(category);
    validRegionName(regionName);

    // region, category, 커서 기준으로 장소를 limit+1개 조회
    List<Place> fetched =
        fetchPlacesByRegionAndCursor(
            userLat, userLng, regionName, category, cursorId, cursorDist, limit);

    // 사용자가 쏠마크한 장소 ID Set 추출
    User user = SecurityUtil.getUser(customUserDetails);
    Set<Long> solmarkedPlaceIds = getSolmarkedPlaceIds(user, fetched);

    // 다음 페이지 커서 값 세팅 (limit+1번째 데이터가 존재할 경우에만)
    CursorInfo next = setNextCursor(fetched, userLat, userLng, limit);

    // 조회된 장소 중 limit개만 PlacePreviewDetail DTO 매핑
    List<PlacePreviewDetail> placePreviewDetails =
        mapToPreviewDetails(fetched, limit, solmarkedPlaceIds);

    return PlaceSearchPreviewResponse.builder()
        .places(placePreviewDetails)
        .nextCursor(next.id())
        .nextCursorDist(next.distance())
        .build();
  }

  private List<Place> fetchPlacesByRegionAndCursor(
      Double userLat,
      Double userLng,
      String regionName,
      String category,
      Long cursorId,
      Double cursorDist,
      int limit) {

    // 거리 계산용 표현식 생성 (Haversine 공식)
    NumberExpression<Double> distance = distance(userLat, userLng);

    return jpaQueryFactory
        .selectFrom(p)
        .distinct()
        .leftJoin(p.placeCategories, pc)
        .leftJoin(pc.category, c)
        .leftJoin(p.placeHours, ph)
        .where(
            regionNameIn(regionName),
            categoryIn(category),
            cursorAfter(cursorId, cursorDist, distance))
        .orderBy(distance.asc(), p.id.asc())
        .limit(limit + 1) // 커서 페이징을 위해 limit+1개 조회 (limit개 + nextCursor용 1개)
        .fetch();
  }

  private CursorInfo setNextCursor(List<Place> places, Double userLat, Double userLng, int limit) {
    Long nextCursor = null;
    Double nextCursorDist = null;

    // limit+1개가 조회되었다면, 마지막 요소를 기준으로 커서 정보 생성
    if (places.size() > limit) {
      nextCursor = places.get(limit - 1).getId();
      Place place = places.get(limit - 1);
      nextCursorDist =
          getNextCursorDistance(userLat, userLng, place.getLatitude(), place.getLongitude());
    }

    return CursorInfo.of(nextCursor, nextCursorDist);
  }

  private List<PlacePreviewDetail> mapToPreviewDetails(
      List<Place> fetched, int limit, Set<Long> solmarkedPlaceIds) {

    return fetched.stream()
        .limit(limit) // limit개만 결과로 반환
        .map(
            p -> {
              List<String> topTagsForPlace =
                  placeRepository.getTopTagsForPlace(p.getId(), TAG_LIMIT);
              List<String> reviewThumbnails =
                  placeRepository.getReviewThumbnails(p.getId(), PREVIEW_THUMBNAIL_LIMIT);
              OpenStatus openStatus = getOpenStatus(p);
              Integer isSoloRecommendedPercent =
                  placeRepository.getRecommendationPercent(p.getId());
              boolean isMarked = solmarkedPlaceIds.contains(p.getId());

              return PlacePreviewDetail.builder()
                  .id(p.getId())
                  .name(p.getName())
                  .detailedCategory(p.getTypes())
                  .tags(topTagsForPlace)
                  .isSoloRecommended(isSoloRecommendedPercent)
                  .rating(truncateTo2Decimals(p.getRating()))
                  .isOpen(openStatus.isOpen())
                  .closingTime(openStatus.closingTime())
                  .isMarked(isMarked)
                  .thumbnailUrls(reviewThumbnails)
                  .build();
            })
        .toList();
  }

  private BooleanExpression regionNameIn(String regionName) {
    return p.district.eq(regionName).or(p.neighborhood.eq(regionName));
  }

  private void validRegionName(String regionName) {
    if (!placeRepository.existsByDistrictOrNeighborhood(regionName, regionName)) {
      throw new CustomException(ErrorCode.REGION_NOT_FOUND);
    }
  }

  @Transactional(readOnly = true)
  public PlaceDetailSearchResponse getPlaceDetail(CustomUserDetails customUserDetails, Long id) {
    User user = SecurityUtil.getUser(customUserDetails);

    Place place =
        placeRepository
            .findByPlaceId(id)
            .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_EXISTS));

    // 현재 영업중 여부 및 마감 시간 반환
    OpenStatus openStatus = getOpenStatus(place);

    // 특정 장소의 리뷰들이 선택한 태그를 MOOD, SOLO로 구분하여 조회후 DTO 생성
    List<TagInfo> moodTagInfo =
        reviewTagCustomRepository.findTagCountsByPlaceAndType(place.getId(), TagType.MOOD);
    List<TagInfo> soloTagInfo =
        reviewTagCustomRepository.findTagCountsByPlaceAndType(place.getId(), TagType.SOLO);
    PlaceTags placeTags = PlaceTags.of(moodTagInfo, soloTagInfo);

    // 추천 비율 퍼센트 계산과 각 리뷰 썸네일 사진 조회
    Integer recommendationPercent = placeRepository.getRecommendationPercent(place.getId());
    List<String> thumbnails =
        placeRepository.getReviewThumbnails(place.getId(), MAX_PLACE_THUMBNAIL_LIMIT);

    // 특정 장소의 쏠마크 개수 조회
    Integer markedCount = solmarkPlaceRepository.countByPlaceAndDeletedAtIsNull(place);

    // 장소 상세 정보 DTO 매핑
    PlaceDetail placeDetail =
        mapToPlaceDetail(
            place, openStatus, placeTags, recommendationPercent, thumbnails, user, markedCount);

    // 특정 장소 리뷰를 최신순, 최대 INITIAL_REVIEW_LIMIT 조회
    PageRequest pageRequest = PageRequest.of(0, INITIAL_REVIEW_LIMIT);
    List<Review> reviews =
        reviewRepository.findByWithImagesAndUserByPlaceId(place.getId(), pageRequest);

    // 리뷰 조회 DTO 매핑
    List<ReviewDetail> reviewDetails = mapToReviewDetail(reviews);

    return PlaceDetailSearchResponse.of(placeDetail, reviewDetails);
  }

  private List<ReviewDetail> mapToReviewDetail(List<Review> reviews) {
    return reviews.stream()
        .map(
            r -> {
              List<String> photoUrls =
                  r.getReviewImages().stream().map(ReviewImage::getImageUrl).toList();
              List<String> tags = r.getReviewTags().stream().map(ReviewTag::getName).toList();

              return ReviewDetail.builder()
                  .userProfileUrl(r.getUser().getProfileImageUrl())
                  .userNickname(r.getUser().getNickname())
                  .createdAt(r.getCreatedAt())
                  .isRecommended(r.getRecommendation())
                  .rating(Double.valueOf(r.getRating()))
                  .content(r.getContent())
                  .photoUrls(photoUrls)
                  .tags(tags)
                  .build();
            })
        .toList();
  }

  private PlaceDetail mapToPlaceDetail(
      Place place,
      OpenStatus openStatus,
      PlaceTags placeTags,
      Integer recommendationPercent,
      List<String> thumbnails,
      User user,
      Integer markedCount) {

    // 특정 장소의 사용자 쏠마크 여부
    boolean isMarked = isPlaceSolmarkedByUser(user, place);

    return PlaceDetail.builder()
        .id(place.getId())
        .name(place.getName())
        .category(getMainCategory(place))
        .detailedCategory(place.getTypes())
        .isMarked(isMarked)
        .markedCount(markedCount)
        .latitude(place.getLatitude())
        .longitude(place.getLongitude())
        .isOpen(openStatus.isOpen())
        .closingTime(openStatus.closingTime())
        .openingHours(getOpeningHours(place))
        .address(place.getAddress())
        .tags(placeTags)
        .isSoloRecommended(recommendationPercent)
        .rating(truncateTo2Decimals(place.getRating())) // 소수점 첫째 자리(0.1 단위)까지 절삭, 두번째 자리 이하 버림
        .thumbnailUrl(thumbnails)
        .build();
  }

  /** 사용자가 특정 장소를 쏠마크(북마크)했는지 판별하는 메서드 */
  private boolean isPlaceSolmarkedByUser(User user, Place place) {
    return getSolmarkedPlaceIds(user, List.of(place)).contains(place.getId());
  }

  private static Double truncateTo2Decimals(Double num) {
    if (num == null) {
      return null;
    }
    // 소수점 첫째 자리(0.1 단위)까지만 남기고 그 이하 버림
    return Math.floor(num * 10) / 10.0;
  }

  private List<OpeningHour> getOpeningHours(Place place) {
    return place.getPlaceHours().stream()
        .map(
            ph ->
                OpeningHour.builder()
                    .dayOfWeek(ph.getDayOfWeek())
                    .startTime(ph.getStartTime())
                    .endTime(ph.getEndTime())
                    .build())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MarkerResponse> getMarkersByRelatedSearch(
      List<Long> ids, CustomUserDetails customUserDetails) {
    // 사용자 로그인, 비로그인 판별
    User user = SecurityUtil.getUser(customUserDetails);

    List<Place> places = placeRepository.findByPlace_IdIn(ids);

    // 쏠마크한 PlaceId 리스트 조회
    Set<Long> solmarkedPlaceIds = getSolmarkedPlaceIds(user, places);

    return places.stream().map(p -> getMarkerResponse(p, solmarkedPlaceIds)).toList();
  }

  @Transactional(readOnly = true)
  public PlaceSearchPreviewResponse getPlacePreviewByRelatedSearch(
      List<Long> ids, Long cursorId, int limit, CustomUserDetails customUserDetails) {

    // 커서 기준으로 시작 인덱스 계산 (없으면 0)
    int startIdx = 0;
    if (cursorId != null) {
      int idx = ids.indexOf(cursorId);
      // cursorId가 ids에 없을 때 -1이 반환됨 -> 그 경우 startIdx는 0 유지
      if (idx != -1) {
        startIdx = idx + 1;
      }
    }
    // limit만큼 끝 인덱스 계산 (리스트 범위 초과 방지)
    int endIdx = Math.min(startIdx + limit, ids.size());

    // 현재 페이지에 해당하는 placeId만 추출
    List<Long> placeIds = ids.subList(startIdx, endIdx);

    // placeId로 Place 조회 (순서 보장 X)
    List<Place> places = placeRepository.findByPlace_IdIn(placeIds);

    // 사용자가 쏠마크한 장소 ID Set 추출
    User user = SecurityUtil.getUser(customUserDetails);
    Set<Long> solmarkedPlaceIds = getSolmarkedPlaceIds(user, places);

    // 조회 결과를 placeIds 순서대로 정렬
    Map<Long, Place> placeMap =
        places.stream().collect(Collectors.toMap(Place::getId, Function.identity()));
    List<Place> orderedPlaces = placeIds.stream().map(placeMap::get).toList();

    // PlacePreviewDetail DTO로 매핑
    List<PlacePreviewDetail> placePreviewDetails =
        mapToPreviewDetails(orderedPlaces, limit, solmarkedPlaceIds);

    // 더 불러올 데이터가 있으면 다음 커서 id, 없으면 null
    Long nextCursor = (endIdx < ids.size()) ? ids.get(endIdx - 1) : null;

    return PlaceSearchPreviewResponse.builder()
        .places(placePreviewDetails)
        .nextCursor(nextCursor)
        .build();
  }

  @Transactional(readOnly = true)
  public PlaceSearchPreviewResponse getPlacesPreviewNearby(
      Double userLat,
      Double userLng,
      Long cursorId,
      Double cursorDist,
      int limit,
      CustomUserDetails customUserDetails) {
    // 반경 km 이내의 장소 조회
    List<Place> places = getPlacesNearby(userLat, userLng, cursorId, cursorDist, limit);

    // 사용자가 쏠마크한 장소 ID Set 추출
    User user = SecurityUtil.getUser(customUserDetails);
    Set<Long> solmarkedPlaceIds = getSolmarkedPlaceIds(user, places);

    // 커서 정보 세팅
    CursorInfo next = setNextCursor(places, userLat, userLng, limit);
    // PlacePreviewDetail DTO 매핑
    List<PlacePreviewDetail> placePreviewDetails =
        mapToPreviewDetails(places, limit, solmarkedPlaceIds);

    return PlaceSearchPreviewResponse.builder()
        .places(placePreviewDetails)
        .nextCursor(next.id())
        .nextCursorDist(next.distance())
        .build();
  }

  private List<Place> getPlacesNearby(
      Double userLat, Double userLng, Long cursorId, Double cursorDist, int limit) {

    NumberExpression<Double> distance = distance(userLat, userLng);

    return jpaQueryFactory
        .selectFrom(p)
        .where(nearby(distance), cursorAfter(cursorId, cursorDist, distance))
        .orderBy(distance.asc(), p.id.asc())
        .limit(limit + 1)
        .fetch();
  }

  private BooleanExpression nearby(NumberExpression<Double> distance) {
    return distance.loe(NEARBY_RADIUS_KM_LIMIT);
  }
}
