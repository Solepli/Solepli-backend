package com.ilta.solepli.domain.solroute.dto.response;

import java.util.List;

import lombok.Builder;

import com.ilta.solepli.domain.solroute.entity.SolroutePlace;

@Builder
public record SolrouteDetailResponse(
    Long id, Integer iconId, String name, Boolean status, List<PlaceInfo> placeInfos) {
  @Builder
  public record PlaceInfo(
      Long placeId,
      Integer seq,
      String placeName,
      String detailedCategory,
      String address,
      String memo,
      String category,
      Double latitude,
      Double longitude) {
    public static PlaceInfo from(SolroutePlace solroutePlace) {
      return PlaceInfo.builder()
          .placeId(solroutePlace.getPlace().getId())
          .seq(solroutePlace.getSeq())
          .placeName(solroutePlace.getPlace().getName())
          .detailedCategory(solroutePlace.getPlace().getTypes())
          .address(solroutePlace.getPlace().getAddress())
          .memo(solroutePlace.getMemo())
          .category(solroutePlace.getPlace().getPlaceCategories().get(0).getCategory().getName())
          .latitude(solroutePlace.getPlace().getLatitude())
          .longitude(solroutePlace.getPlace().getLongitude())
          .build();
    }
  }
}
