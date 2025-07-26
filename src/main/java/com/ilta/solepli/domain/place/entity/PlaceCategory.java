package com.ilta.solepli.domain.place.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.ilta.solepli.domain.category.entity.Category;
import com.ilta.solepli.global.entity.Timestamped;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "place_categories")
public class PlaceCategory extends Timestamped {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id")
  @OnDelete(action = OnDeleteAction.CASCADE) // Place 삭제시 PlaceCategory 삭제
  private Place place;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  @OnDelete(action = OnDeleteAction.CASCADE) // Category 삭제시 PlaceCategory 삭제
  private Category category;

  protected void setPlace(Place place) {
    this.place = place;
  }
}
