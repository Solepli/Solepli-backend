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
    Double longitude) {}
