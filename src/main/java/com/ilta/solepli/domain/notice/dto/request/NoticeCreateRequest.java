package com.ilta.solepli.domain.notice.dto.request;

import jakarta.validation.constraints.NotNull;

import com.ilta.solepli.domain.notice.entity.Notice;

public record NoticeCreateRequest(
    @NotNull(message = "제목은 필수입니다.") String title,
    @NotNull(message = "내용은 필수입니다.") String content) {
  public Notice toEntity() {
    return Notice.builder().title(title).content(content).build();
  }
}
