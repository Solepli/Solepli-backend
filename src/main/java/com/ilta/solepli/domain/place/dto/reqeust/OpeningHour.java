package com.ilta.solepli.domain.place.dto.reqeust;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record OpeningHour(
    @NotNull(message = "요일은 필수 입니다.") Integer dayOfWeek,
    @NotNull(message = "시작 시간은 필수입니다.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime startTime,
    @NotNull(message = "종료 시간은 필수입니다.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        LocalTime endTime) {}
