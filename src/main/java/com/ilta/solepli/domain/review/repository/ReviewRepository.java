package com.ilta.solepli.domain.review.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ilta.solepli.domain.place.entity.Place;
import com.ilta.solepli.domain.review.entity.Review;
import com.ilta.solepli.domain.user.entity.User;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.place.id = :placeId")
  Double findAverageRatingByPlaceId(@Param("placeId") Long placeId);

  boolean existsByUserAndPlace(User user, Place place);

  @Query(
      """
                      SELECT r
                      FROM Review r
                      JOIN FETCH r.user u
                      JOIN FETCH r.reviewImages ri
                      WHERE r.place.id = :placeId
                      ORDER BY r.createdAt DESC
                  """)
  List<Review> findByWithImagesAndUserByPlaceId(@Param("placeId") Long placeId, Pageable pageable);

  @Query("SELECT r FROM Review r JOIN FETCH r.reviewImages WHERE r.user = :user")
  List<Review> findAllByUserWithImages(@Param("user") User user);
}
