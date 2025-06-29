package com.ilta.solepli.domain.notice.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ilta.solepli.domain.notice.entity.Notice;

public record NoticePreviewResponse(
    String title,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDateTime createdAt) {
  public static NoticePreviewResponse from(Notice notice) {
    return new NoticePreviewResponse(notice.getTitle(), notice.getCreatedAt());
  }
}
