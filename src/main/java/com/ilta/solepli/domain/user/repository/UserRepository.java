package com.ilta.solepli.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ilta.solepli.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByLoginId(String loginId);

  Boolean existsByLoginId(String loginId);

  Boolean existsByNickname(String nickname);

  @Query(
      value =
          "SELECT * "
              + "FROM users "
              + "WHERE nickname LIKE CONCAT(:prefix, '%') "
              + "ORDER BY CAST(SUBSTRING(nickname, CHAR_LENGTH(:prefix) + 1) AS UNSIGNED) DESC "
              + "LIMIT 1",
      nativeQuery = true)
  Optional<User> findTopByNicknameStartingWithOrderByNicknameDesc(String prefix);
}
