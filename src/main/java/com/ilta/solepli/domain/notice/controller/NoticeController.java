package com.ilta.solepli.domain.notice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.notice.dto.request.NoticeCreateRequest;
import com.ilta.solepli.domain.notice.dto.request.NoticeUpdateRequest;
import com.ilta.solepli.domain.notice.service.NoticeService;
import com.ilta.solepli.global.response.SuccessResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
@Tag(name = "NoticeController", description = "공지사항 관련 API")
public class NoticeController {

  private final NoticeService noticeService;

  @Operation(summary = "공지사항 등록 API", description = "공지사항을 등록하는 API입니다. 관리자만 가능합니다.")
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<SuccessResponse<Void>> createNotice(
      @Valid @RequestBody NoticeCreateRequest request) {

    noticeService.createNotice(request);

    return ResponseEntity.status(201).body(SuccessResponse.successWithNoData("공지사항 등록 성공"));
  }

  @Operation(summary = "공지사항 수정 API", description = "공지사항을 수정하는 API입니다. 관리자만 가능합니다.")
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{id}")
  public ResponseEntity<SuccessResponse<Void>> updateNotice(
      @RequestBody NoticeUpdateRequest request, @PathVariable Long id) {

    noticeService.updateNotice(request, id);

    return ResponseEntity.ok().body(SuccessResponse.successWithNoData("공지사항 수정 성공"));
  }

  @Operation(summary = "공지사항 삭제 API", description = "공지사항을 삭제하는 API입니다. 관리자만 가능합니다.")
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<Void>> deleteNotice(@PathVariable Long id) {

    noticeService.deleteNotice(id);

    return ResponseEntity.ok().body(SuccessResponse.successWithNoData("공지사항 삭제 성공"));
  }
}
