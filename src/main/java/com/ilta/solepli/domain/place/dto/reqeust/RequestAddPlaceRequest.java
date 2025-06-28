package com.ilta.solepli.domain.place.dto.reqeust;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestAddPlaceRequest(
    @NotBlank(message = "장소명은 필수 입니다.") @Size(max = 30, message = "장소명은 최대 30자 입니다.")
        String placeName,
    @NotBlank(message = "주소는 필수 입니다.") @Size(max = 50, message = "주소명은 최대 50자 입니다.") String address,
    @Size(min = 1, message = "카테고리는 1개 이상 선택 해주세요.") List<String> category,
    @Size(max = 500, message = "추가 메모는 최대 500자 입니다.") String note) {}
