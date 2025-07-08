package com.ilta.solepli.domain.solroute.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record PlaceSummaryResponse(
    Long id,
    String name,
    String detailedCategory,
    String category,
    String address,
    Double latitude,
    Double longitude,
    Integer recommendationPercent,
    List<String> tags,
    Double rating) {}
