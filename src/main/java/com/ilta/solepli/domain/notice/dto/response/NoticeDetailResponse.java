package com.ilta.solepli.domain.notice.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ilta.solepli.domain.notice.entity.Notice;

public record NoticeDetailResponse(
    Long id,
    String title,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDateTime createdAt,
    String content) {
  public static NoticeDetailResponse from(Notice notice) {
    return new NoticeDetailResponse(
        notice.getId(), notice.getTitle(), notice.getCreatedAt(), notice.getTitle());
  }
}
