package com.ilta.solepli.domain.solroute.dto.response;

import lombok.Builder;

@Builder
public record PlacePreviewResponse(
    Long placeId,
    String placeName,
    String detailedCategory,
    String address,
    String category,
    Double latitude,
    Double longitude) {}
