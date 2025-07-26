package com.ilta.solepli.domain.place.dto.reqeust;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddPlaceRequest(
    @NotBlank(message = "장소명은 필수 입니다.") @Size(max = 30, message = "장소명은 최대 30자 입니다.")
        String placeName,
    @NotBlank(message = "주소는 필수 입니다.") @Size(max = 50, message = "주소명은 최대 50자 입니다.") String address,
    @Size(min = 1, message = "카테고리는 1개 이상 선택 해주세요.")
        List<@NotBlank(message = "카테고리 명은 공백일 수 없습니다.") String> category,
    @NotBlank(message = "시·군·구는 필수 입니다.") String district,
    String neighborhood,
    @NotNull(message = "위도 값은 필수 입니다.") Double latitude,
    @NotNull(message = "경도 값은 필수 입니다.") Double longitude,
    @NotBlank(message = "상세분류명은 필수 입니다.") String types,
    @Valid List<OpeningHour> openingHours) {}
