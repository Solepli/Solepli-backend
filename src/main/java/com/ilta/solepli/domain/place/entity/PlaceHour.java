package com.ilta.solepli.domain.place.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
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

import com.ilta.solepli.global.entity.Timestamped;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "place_hours")
public class PlaceHour extends Timestamped {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, name = "day_of_week") // 0:일요일 ~ 6:토요일
  private Integer dayOfWeek;

  @Column(nullable = false, name = "start_time")
  private LocalTime startTime;

  @Column(nullable = false, name = "end_time")
  private LocalTime endTime;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id")
  private Place place;

  protected void setPlace(Place place) {
    this.place = place;
  }
}
