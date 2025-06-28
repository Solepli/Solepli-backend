package com.ilta.solepli.domain.sollect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ilta.solepli.domain.sollect.entity.Sollect;
import com.ilta.solepli.domain.user.entity.User;

public interface SollectRepository extends JpaRepository<Sollect, Long> {
  @EntityGraph(attributePaths = {"user", "sollectContents"})
  @Query("SELECT s FROM Sollect s WHERE s.id = :id AND s.deletedAt IS NULL")
  Optional<Sollect> findWithContentById(@Param("id") Long id);

  @Query("select s.id from Sollect s where s.user = :user AND s.deletedAt IS NULL")
  List<Long> findSollectIdsByUser(@Param("user") User user);

  @EntityGraph(attributePaths = {"sollectContents"})
  List<Sollect> findAllByUser(User user);
}
