package com.ilta.solepli.domain.place.dto.response;

import lombok.Builder;

@Builder
public record PlaceSearchResponse(
    Long id,
    String name,
    String category,
    String detailedCategory,
    String address,
    Double latitude,
    Double longitude,
    Boolean isMarked) {
  public static PlaceSearchResponse from(PlaceSearchResponseDTO dto, Boolean isMarked) {
    return new PlaceSearchResponse(
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
