package com.ilta.solepli.domain.solmap.dto;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

@Builder
public record PlacePreviewDetail(
    Long id,
    String name,
    String detailedCategory,
    List<String> tags,
    Integer isSoloRecommended,
    Double rating,
    Boolean isOpen,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime closingTime,
    Boolean isMarked,
    List<String> thumbnailUrls) {}
