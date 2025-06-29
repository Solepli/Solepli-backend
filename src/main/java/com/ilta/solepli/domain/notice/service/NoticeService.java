package com.ilta.solepli.domain.notice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.notice.dto.request.NoticeCreateRequest;
import com.ilta.solepli.domain.notice.entity.Notice;
import com.ilta.solepli.domain.notice.repository.NoticeRepository;

@Service
@RequiredArgsConstructor
public class NoticeService {

  private final NoticeRepository noticeRepository;

  @Transactional
  public void createNotice(NoticeCreateRequest request) {
    Notice notice = request.toEntity();

    noticeRepository.save(notice);
  }
}
