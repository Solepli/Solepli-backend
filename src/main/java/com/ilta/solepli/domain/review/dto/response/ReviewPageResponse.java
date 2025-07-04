package com.ilta.solepli.domain.review.dto.response;

import java.util.List;

public record ReviewPageResponse(String placeName, List<ReviewDetail> reviews, Long nextCursor) {
  public static ReviewPageResponse of(
      String placeName, List<ReviewDetail> reviews, Long nextCursor) {
    return new ReviewPageResponse(placeName, reviews, nextCursor);
  }
}
