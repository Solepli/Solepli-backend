package com.ilta.solepli.domain.review.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.place.entity.Place;
import com.ilta.solepli.domain.place.entity.QPlace;
import com.ilta.solepli.domain.place.repository.PlaceRepository;
import com.ilta.solepli.domain.review.dto.request.ReviewCreateRequest;
import com.ilta.solepli.domain.review.dto.response.ReviewDetail;
import com.ilta.solepli.domain.review.dto.response.ReviewPageResponse;
import com.ilta.solepli.domain.review.entity.QReview;
import com.ilta.solepli.domain.review.entity.Review;
import com.ilta.solepli.domain.review.entity.mapping.ReviewImage;
import com.ilta.solepli.domain.review.entity.mapping.ReviewTag;
import com.ilta.solepli.domain.review.repository.ReviewRepository;
import com.ilta.solepli.domain.tag.entity.MoodTag;
import com.ilta.solepli.domain.tag.entity.SoloTag;
import com.ilta.solepli.domain.tag.entity.TagType;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.service.S3Service;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final PlaceRepository placeRepository;
  private final ReviewRepository reviewRepository;
  private final S3Service s3Service;
  private final JPAQueryFactory jpaQueryFactory;

  private final QReview r = QReview.review;
  private final QPlace p = QPlace.place;

  @Transactional
  public void createReview(ReviewCreateRequest request, List<MultipartFile> files, User user) {
    Place place =
        placeRepository
            .findById(request.placeId())
            .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_EXISTS));

    if (reviewRepository.existsByUserAndPlace(user, place)) {
      throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // 리뷰 저장
    Review review =
        Review.builder()
            .recommendation(request.recommendation())
            .rating(request.rating())
            .content(request.content())
            .place(place)
            .user(user)
            .build();

    // 리뷰 태그 저장
    List<ReviewTag> reviewTags = new ArrayList<>();

    // 분위기 태그
    for (String tag : request.moodTag()) {
      if (!MoodTag.isValid(tag)) {
        throw new CustomException(ErrorCode.TAG_NOT_EXISTS);
      }
      reviewTags.add(ReviewTag.builder().name(tag).review(review).tagType(TagType.MOOD).build());
    }

    // 1인 이용 태그
    for (String tag : request.soloTag()) {
      if (!SoloTag.isValid(tag)) {
        throw new CustomException(ErrorCode.TAG_NOT_EXISTS);
      }
      reviewTags.add(ReviewTag.builder().name(tag).review(review).tagType(TagType.SOLO).build());
    }
    review.getReviewTags().addAll(reviewTags);

    // 리뷰 이미지 저장
    if (files != null) {
      if (files.size() > 5) {
        throw new CustomException(ErrorCode.TOO_MANY_REVIEW_IMAGES);
      }

      List<ReviewImage> reviewImages = new ArrayList<>();
      for (MultipartFile file : files) {
        if (file.getSize() > 5 * 1024 * 1024) {
          throw new CustomException(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }
        String imageUrl = s3Service.uploadReviewImage(file);
        ReviewImage reviewImage = ReviewImage.builder().imageUrl(imageUrl).review(review).build();
        reviewImages.add(reviewImage);
      }
      review.getReviewImages().addAll(reviewImages);
    }

    reviewRepository.save(review);
    reviewRepository.flush(); // 강제로 insert 쿼리 실행

    // 리뷰 평점 계산
    place.updateRating(reviewRepository.findAverageRatingByPlaceId(place.getId()));
  }

  @Transactional(readOnly = true)
  public ReviewPageResponse getReviewDetails(Long id, Long cursorId, int limit) {

    // 장소(placeId), 리뷰(cursorId) 검증
    validPlace(id);
    if (cursorId != null) {
      validReview(cursorId);
    }

    // cursorId를 기반으로 리뷰 조회(limit + 1)
    List<Review> reviews = getReviews(id, cursorId, limit);

    // 조회된 리뷰가 limit + 1 크기일경우 nextCursor 세팅
    Long nextCursor = null;
    if (reviews.size() > limit) {
      reviews.remove(reviews.size() - 1);
      nextCursor = reviews.get(reviews.size() - 1).getId();
    }

    // ReviewDetail DTO 매핑
    List<ReviewDetail> reviewDetails = mapToReviewDetail(reviews);

    return ReviewPageResponse.of(reviewDetails, nextCursor);
  }

  private void validReview(Long reviewId) {
    if (!reviewRepository.existsById(reviewId)) {
      throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
    }
  }

  private void validPlace(Long id) {
    if (!placeRepository.existsById(id)) {
      throw new CustomException(ErrorCode.PLACE_NOT_EXISTS);
    }
  }

  private List<Review> getReviews(Long placeId, Long cursorId, int limit) {

    List<Long> ids =
        jpaQueryFactory
            .select(r.id)
            .from(r)
            .where(placeIdEq(placeId).and(reviewIdLT(cursorId)).and(r.deletedAt.isNull()))
            .orderBy(r.id.desc())
            .limit(limit + 1)
            .fetch();

    return jpaQueryFactory
        .selectFrom(r)
        .distinct()
        .leftJoin(r.reviewImages)
        .fetchJoin()
        .join(r.user)
        .fetchJoin()
        .where(r.id.in(ids))
        .orderBy(r.id.desc())
        .fetch();
  }

  private BooleanExpression placeIdEq(Long placeId) {
    return p.id.eq(placeId);
  }

  private BooleanExpression reviewIdLT(Long reviewId) {
    if (reviewId == null) {
      return null;
    }
    return r.id.lt(reviewId);
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
}
