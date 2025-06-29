package com.ilta.solepli.domain.solmap.dto;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

@Builder
public record PlaceDetail(
    Long id,
    String name,
    String category,
    String detailedCategory,
    Boolean isMarked,
    Integer markedCount,
    Double latitude,
    Double longitude,
    Boolean isOpen,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime closingTime,
    List<OpeningHour> openingHours,
    String address,
    PlaceTags tags,
    Integer isSoloRecommended,
    Double rating,
    List<String> thumbnailUrl) {}
