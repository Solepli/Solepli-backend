package com.ilta.solepli.domain.solroute.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record SolrouteCreateRequest(
    @NotNull(message = "아이콘 ID는 필수입니다.") Integer iconId,
    @NotBlank(message = "제목은 필수입니다.") @Size(max = 25, message = "제목은 25자 이하로 입력해주세요.") String name,
    @NotNull(message = "장소 정보는 필수입니다.")
        @Size(max = 50, message = "장소 정보는 최대 50개까지 등록할 수 있습니다.")
        @Valid
        List<PlaceInfo> placeInfos) {
  public record PlaceInfo(
      @NotNull(message = "장소 ID는 필수입니다.") Long id,
      @NotNull(message = "순서는 필수입니다.") Integer seq,
      @Size(max = 100, message = "메모는 최대 100자까지 입력할 수 있습니다.") String memo) {}
}
