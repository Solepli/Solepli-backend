package com.ilta.solepli.domain.solroute.dto.response;

import lombok.Builder;

import com.ilta.solepli.domain.solroute.entity.Solroute;

@Builder
public record SolroutePreviewResponse(
    Long id, Integer iconId, String name, Integer placeCount, Boolean status) {
  public static SolroutePreviewResponse from(Solroute solroute) {
    return SolroutePreviewResponse.builder()
        .id(solroute.getId())
        .name(solroute.getName())
        .iconId(solroute.getIconId())
        .placeCount(solroute.getSolroutePlaces().size())
        .status(solroute.getStatus())
        .build();
  }
}
