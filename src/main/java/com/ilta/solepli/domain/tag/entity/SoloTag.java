package com.ilta.solepli.domain.tag.entity;

import java.util.Arrays;

import lombok.Getter;

import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;

@Getter
public enum SoloTag {
  HAS_SOLO_SEATS("1인 좌석이 있는"),
  HAS_SOLO_MENU("1인 메뉴가 있는"),
  MANY_CONCENT("콘센트가 많은"),
  GOOD_FOR_LONG_STAY("오래 머물기 좋은"),
  EASY_TO_CARRY("가볍게 들르기 좋은"),
  GOOD_FOR_DAY("낮에 가기 좋은"),
  GOOD_FOR_NIGHT("밤에 가기 좋은"),
  SPACIOUS("넓은"),
  SMALL("좁은");

  private final String description;

  SoloTag(String description) {
    this.description = description;
  }

  public static SoloTag fromDescription(String description) {
    for (SoloTag tag : SoloTag.values()) {
      if (tag.getDescription().equals(description)) {
        return tag;
      }
    }
    throw new CustomException(ErrorCode.TAG_NOT_EXISTS);
  }

  public static boolean isValid(String description) {
    return Arrays.stream(values()).anyMatch(tag -> tag.getDescription().equals(description));
  }
}
