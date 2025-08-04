package com.ilta.solepli.domain.solroute.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ilta.solepli.domain.solroute.entity.Solroute;
import com.ilta.solepli.domain.user.entity.User;

public interface SolrouteRepository extends JpaRepository<Solroute, Long> {
  @Query("SELECT s FROM Solroute s WHERE s.id = :id AND s.user = :user AND s.deletedAt IS NULL")
  Optional<Solroute> findByIdAndUser(Long id, User user);

  @EntityGraph(attributePaths = {"solroutePlaces", "solroutePlaces.place"})
  @Query("SELECT s FROM Solroute s WHERE s.id = :id AND s.user = :user AND s.deletedAt IS NULL")
  Optional<Solroute> findByIdAndUserWithPlaces(Long id, User user);

  @EntityGraph(attributePaths = {"solroutePlaces"})
  @Query(
      "SELECT s FROM Solroute s WHERE s.user = :user AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
  List<Solroute> findAllByUserId(User user);

  Optional<Solroute> findTop1ByUserAndNameStartingWithOrderByIdDesc(User user, String name);
}
