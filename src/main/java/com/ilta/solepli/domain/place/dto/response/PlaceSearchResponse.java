package com.ilta.solepli.domain.place.dto.response;

import java.util.List;

import lombok.Builder;

import com.ilta.solepli.domain.sollect.dto.response.SollectSearchResponse.CursorInfo;

@Builder
public record PlaceSearchResponse(List<PlaceSearchContent> contents, CursorInfo cursorInfo) {

  @Builder
  public record PlaceSearchContent(
      Long id,
      String name,
      String category,
      String detailedCategory,
      String address,
      Double latitude,
      Double longitude,
      Boolean isMarked) {
    public static PlaceSearchContent from(PlaceSearchResponseDTO dto, Boolean isMarked) {
      return new PlaceSearchContent(
          dto.id(),
          dto.name(),
          dto.category(),
          dto.detailedCategory(),
          dto.address(),
          dto.latitude(),
          dto.longitude(),
          isMarked);
    }
  }

  @Builder
  public record cursorInfo(Long nextCursorId, boolean hasNext) {}
}
