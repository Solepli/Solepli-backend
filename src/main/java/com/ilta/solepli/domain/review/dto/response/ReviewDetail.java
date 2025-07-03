package com.ilta.solepli.domain.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

@Builder
public record ReviewDetail(
    String userProfileUrl,
    String userNickname,
    LocalDateTime createdAt,
    Boolean isRecommended,
    Double rating,
    String content,
    List<String> photoUrls,
    List<String> tags) {}
