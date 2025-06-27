package com.ilta.solepli.domain.sollect.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import com.ilta.solepli.domain.sollect.entity.ContentType;

@Builder
public record SollectDetailResponse(
    String thumbnailImageUrl,
    String title,
    String placeName,
    Integer otherPlaceCount,
    String district,
    String neighborhood,
    String profileImageUrl,
    String nickname,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
    List<SollectContent> contents,
    Long markedCount,
    List<PlaceSummary> placeSummaries) {
  @Builder
  public record SollectContent(ContentType type, String content) {}

  @Builder
  public record PlaceSummary(
      Long placeId,
      String name,
      String detailedCategory,
      Integer recommendationPercent,
      List<String> tags,
      Boolean isMarked,
      Double rating) {}
}
