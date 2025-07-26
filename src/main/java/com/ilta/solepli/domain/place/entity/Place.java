package com.ilta.solepli.domain.place.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

import com.ilta.solepli.domain.review.entity.Review;
import com.ilta.solepli.global.entity.Timestamped;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "places")
public class Place extends Timestamped {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false)
  private String district;

  private String neighborhood;

  @Column(nullable = false)
  private Double latitude;

  @Column(nullable = false)
  private Double longitude;

  @Column(nullable = false, name = "google_place_id")
  private String googlePlaceId;

  @Column(nullable = false)
  private String types;

  private Double rating;

  @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @Builder.Default
  private List<PlaceCategory> placeCategories = new ArrayList<>();

  @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @Builder.Default
  private List<PlaceHour> placeHours = new ArrayList<>();

  @OneToMany(mappedBy = "place", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @Builder.Default
  private List<Review> reviews = new ArrayList<>();

  public void updateRating(Double rating) {
    this.rating = rating;
  }

  public void addPlaceHour(PlaceHour placeHour) {
    this.placeHours.add(placeHour);
    placeHour.setPlace(this);
  }

  public void addPlaceCategory(PlaceCategory placeCategory) {
    this.placeCategories.add(placeCategory);
    placeCategory.setPlace(this);
  }
}
