package com.ilta.solepli.domain.solroute.dto.response;

import lombok.Builder;

@Builder
public record PlacePreviewResponse(
    Long placeId, String name, String detailedCategory, String address) {}
