package com.ilta.solepli.domain.place.repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.category.entity.QCategory;
import com.ilta.solepli.domain.place.dto.response.PlaceSearchResponseDTO;
import com.ilta.solepli.domain.place.entity.Place;
import com.ilta.solepli.domain.place.entity.QPlace;
import com.ilta.solepli.domain.place.entity.QPlaceCategory;
import com.ilta.solepli.domain.place.entity.SearchType;
import com.ilta.solepli.domain.review.entity.QReview;
import com.ilta.solepli.domain.review.entity.Review;
import com.ilta.solepli.domain.review.entity.mapping.QReviewImage;
import com.ilta.solepli.domain.review.entity.mapping.QReviewTag;
import com.ilta.solepli.domain.review.entity.mapping.ReviewImage;
import com.ilta.solepli.domain.sollect.dto.response.SollectPlaceAddPreviewResponse;

@RequiredArgsConstructor
public class PlaceRepositoryImpl implements PlaceRepositoryCustom {

  private final JPAQueryFactory jpaQueryFactory;
  private final QReviewTag rt = QReviewTag.reviewTag;
  private final QReview r = QReview.review;
  private final QReviewImage ri = QReviewImage.reviewImage;
  private final QPlace p = QPlace.place;
  private final QPlaceCategory pc = QPlaceCategory.placeCategory;
  private final QCategory c = QCategory.category;

  // 장소별 최다 리뷰 태그 n개 조회
  @Override
  public List<String> getTopTagsForPlace(Long placeId, int limit) {
    return jpaQueryFactory
        .select(rt.name)
        .from(rt)
        .join(rt.review, r)
        .where(rt.review.place.id.eq(placeId))
        .groupBy(rt.name)
        .orderBy(rt.id.count().desc(), rt.name.asc())
        .limit(limit)
        .fetch();
  }

  // 장소별 최신 리뷰의 썸네일 이미지 n개 조회
  @Override
  public List<String> getReviewThumbnails(Long placeId, int limit) {
    List<Review> reviews =
        jpaQueryFactory
            .selectFrom(r)
            .distinct()
            .join(r.reviewImages, ri)
            .where(r.place.id.eq(placeId))
            .orderBy(r.createdAt.desc())
            .limit(limit)
            .fetch();

    return reviews.stream()
        .map(
            r ->
                r.getReviewImages().stream().findFirst().map(ReviewImage::getImageUrl).orElse(null))
        .filter(Objects::nonNull)
        .toList();
  }

  // 장소별 추천 비율 반환 (0~100)
  @Override
  public Integer getRecommendationPercent(Long placeId) {
    Long recommendedCountObj =
        jpaQueryFactory
            .select(r.count())
            .from(r)
            .where(r.place.id.eq(placeId).and(r.recommendation.eq(true)))
            .fetchOne();

    Long totalCountObj =
        jpaQueryFactory.select(r.count()).from(r).where(r.place.id.eq(placeId)).fetchOne();

    long recommendedCount = (recommendedCountObj == null) ? 0L : recommendedCountObj;
    long totalCount = (totalCountObj == null) ? 0L : totalCountObj;

    if (totalCount == 0) {
      return null;
    }

    double percent = recommendedCount * 100.0 / totalCount;
    return (int) percent;
  }

  @Override
  public List<PlaceSearchResponseDTO> getPlacesByKeyword(
      String keyword, Double userLat, Double userLng, SearchType searchType, Long limit) {

    // 0) 검색 조건
    BooleanBuilder cond = new BooleanBuilder();
    if (searchType == SearchType.PLACE_NAME) cond.and(p.name.contains(keyword));
    else cond.and(p.address.contains(keyword));

    long k = (limit == null ? 10L : limit);

    // 좌표가 있으면 거리순 정렬
    if (userLat != null && userLng != null) {
      NumberExpression<Double> dist = distance(userLat, userLng);
      List<Tuple> rows =
          jpaQueryFactory
              .select(p.id, p.name, p.types, p.address, p.latitude, p.longitude)
              .from(p)
              .where(cond)
              .orderBy(dist.asc(), p.id.asc())
              .limit(k)
              .fetch();

      return assemblePlaceDTO(rows);
    }

    // 좌표가 없을 경우, 전부 가져온 다음, 섞고 자르기
    List<Tuple> rows =
        jpaQueryFactory
            .select(p.id, p.name, p.types, p.address, p.latitude, p.longitude)
            .from(p)
            .where(cond)
            .orderBy(p.id.asc())
            .fetch();

    Collections.shuffle(rows);
    if (rows.size() > k) rows = rows.subList(0, (int) k);

    return assemblePlaceDTO(rows);
  }

  @Override
  public SollectPlaceAddPreviewResponse getSollectAddPreview(Long placeId) {
    Place place =
        jpaQueryFactory
            .select(p)
            .from(p)
            .join(p.placeCategories, pc)
            .join(pc.category, c)
            .where(p.id.eq(placeId))
            .fetchOne();

    return SollectPlaceAddPreviewResponse.builder()
        .id(place.getId())
        .name(place.getName())
        .address(place.getAddress())
        .category(getMainCategory(place))
        .build();
  }

  /** 장소에 연결된 카테고리 중 첫 번째(대표) 카테고리명을 반환. */
  private String getMainCategory(Place place) {
    return place.getPlaceCategories().get(0).getCategory().getName();
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

  // 메인 카테고리 + DTO 조립
  private List<PlaceSearchResponseDTO> assemblePlaceDTO(List<Tuple> rows) {
    if (rows.isEmpty()) return List.of();
    List<Long> placeIds = rows.stream().map(t -> t.get(p.id)).toList();

    List<Tuple> cats =
        jpaQueryFactory
            .select(pc.place.id, pc.id, c.name)
            .from(pc)
            .join(pc.category, c)
            .where(pc.place.id.in(placeIds))
            .orderBy(pc.place.id.asc(), pc.id.asc())
            .fetch();

    Map<Long, String> mainCategoryMap = new LinkedHashMap<>();
    for (Tuple t : cats) {
      Long placeId = t.get(pc.place.id);
      mainCategoryMap.putIfAbsent(placeId, t.get(c.name));
    }

    return rows.stream()
        .map(
            t -> {
              Long id = t.get(p.id);
              return PlaceSearchResponseDTO.builder()
                  .id(id)
                  .name(t.get(p.name))
                  .category(mainCategoryMap.get(id))
                  .detailedCategory(t.get(p.types))
                  .address(t.get(p.address))
                  .latitude(t.get(p.latitude))
                  .longitude(t.get(p.longitude))
                  .build();
            })
        .toList();
  }

  // 전체 Place 테이블의 id 범위를 저장할 자료형 정의
  private static class IdRange {
    long minId, maxId; // minId: 가장 작은 id, maxId: 가장 큰 id
  }

  // 스레드 간 가시성을 보장하면서 placeIdRangeCache 변수 캐싱 (lazy 초기화용)
  private volatile IdRange placeIdRangeCache; // volatile: 값이 바뀌면 다른 스레드도 즉시 반영된 값을 볼 수 있음

  // placeIdRangeCache가 없으면 min/max를 계산해서 채우고, 있으면 캐시된 값 반환
  private IdRange getPlaceIdRange() {
    // 1) 먼저 캐시된 값이 있는지 확인
    IdRange cached = placeIdRangeCache;
    if (cached != null) return cached; // 있으면 그대로 반환

    // 2) 없으면 DB에서 직접 min/max id를 계산
    Long minId = jpaQueryFactory.select(p.id.min()).from(p).fetchOne(); // 최솟값
    Long maxId = jpaQueryFactory.select(p.id.max()).from(p).fetchOne(); // 최댓값

    // 3) null 방지: 테이블이 비어 있을 경우 0으로 기본값 대체
    cached = new IdRange();
    cached.minId = minId == null ? 0L : minId;
    cached.maxId = maxId == null ? 0L : maxId;

    // 4) 계산된 범위를 캐시에 저장 (다음 호출 시 DB 쿼리 생략 가능)
    placeIdRangeCache = cached;

    // 5) 새로 계산한 범위 반환
    return cached;
  }
}
