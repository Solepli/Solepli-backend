package com.ilta.solepli.domain.solmap.dto;

import java.util.List;

import com.ilta.solepli.domain.review.dto.response.ReviewDetail;

public record PlaceDetailSearchResponse(PlaceDetail place, List<ReviewDetail> reviews) {
  public static PlaceDetailSearchResponse of(PlaceDetail place, List<ReviewDetail> reviews) {
    return new PlaceDetailSearchResponse(place, reviews);
  }
}
