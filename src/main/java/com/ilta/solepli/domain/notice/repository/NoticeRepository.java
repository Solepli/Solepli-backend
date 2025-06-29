package com.ilta.solepli.domain.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilta.solepli.domain.notice.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {}
