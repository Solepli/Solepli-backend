package com.ilta.solepli.domain.place.repository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.category.entity.QCategory;
import com.ilta.solepli.domain.place.dto.response.PlaceSearchResponseDTO;
import com.ilta.solepli.domain.place.entity.Place;
import com.ilta.solepli.domain.place.entity.QPlace;
import com.ilta.solepli.domain.place.entity.QPlaceCategory;
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
      Long cursorId, Integer size, String keyword) {

    BooleanBuilder cond = new BooleanBuilder();
    if (keyword != null && !keyword.isBlank()) {
      cond.and(p.name.contains(keyword).or(p.address.contains(keyword)));
    }
    if (cursorId != null) {
      cond.and(p.id.lt(cursorId));
    }

    return jpaQueryFactory
        .select(p)
        .distinct()
        .from(p)
        .leftJoin(p.placeCategories, pc)
        .fetchJoin()
        .leftJoin(pc.category, c)
        .fetchJoin()
        .where(cond)
        .orderBy(p.id.desc())
        .limit(size + 1) // 커서 페이징이므로 size+1
        .fetch()
        .stream()
        .map(
            p ->
                PlaceSearchResponseDTO.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .category(getMainCategory(p))
                    .detailedCategory(p.getTypes())
                    .address(p.getAddress())
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .build())
        .collect(Collectors.toList());
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
}
