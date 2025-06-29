package com.ilta.solepli.domain.solroute.dto.response;

import lombok.Builder;

@Builder
public record PlacePreviewResponse(Long id, String name, String detailedCategory, String address) {}
