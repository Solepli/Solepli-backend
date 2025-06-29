package com.ilta.solepli.domain.notice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ilta.solepli.domain.notice.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
  @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL ORDER BY n.createdAt DESC")
  List<Notice> findAllNotices();
}
