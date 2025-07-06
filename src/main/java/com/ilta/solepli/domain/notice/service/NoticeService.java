package com.ilta.solepli.domain.notice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.notice.dto.request.NoticeCreateRequest;
import com.ilta.solepli.domain.notice.dto.request.NoticeUpdateRequest;
import com.ilta.solepli.domain.notice.dto.response.NoticeDetailResponse;
import com.ilta.solepli.domain.notice.dto.response.NoticePreviewResponse;
import com.ilta.solepli.domain.notice.entity.Notice;
import com.ilta.solepli.domain.notice.repository.NoticeRepository;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class NoticeService {

  private final NoticeRepository noticeRepository;

  @Transactional
  public void createNotice(NoticeCreateRequest request) {
    Notice notice = request.toEntity();

    noticeRepository.save(notice);
  }

  @Transactional
  public void updateNotice(NoticeUpdateRequest request, Long id) {
    Notice notice =
        noticeRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

    notice.update(request);
  }

  @Transactional
  public void deleteNotice(Long id) {
    Notice notice =
        noticeRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

    //    notice.softDelete();
    noticeRepository.delete(notice);
  }

  @Transactional(readOnly = true)
  public List<NoticePreviewResponse> getNotices() {
    List<Notice> notices = noticeRepository.findAllNotices();

    return notices.stream().map(NoticePreviewResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public NoticeDetailResponse getNoticeDetail(Long id) {
    Notice notice =
        noticeRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

    return NoticeDetailResponse.from(notice);
  }
}
