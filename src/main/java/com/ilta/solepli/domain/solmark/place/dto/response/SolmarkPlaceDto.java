package com.ilta.solepli.domain.solmark.place.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record SolmarkPlaceDto(
    Long id,
    String name,
    String detailedCategory,
    Integer recommendationPercent,
    List<String> tags,
    Double rating,
    String category,
    String address,
    Double latitude,
    Double longitude) {}
