package com.ilta.solepli.domain.profile.validator;

import java.util.regex.Pattern;

public class NicknameValidator {
  private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣._\\-]+$");

  public static boolean isValidFormat(String nickname) {
    return VALID_PATTERN.matcher(nickname).matches();
  }

  public static boolean isLengthValid(String nickname) {
    return nickname != null && nickname.codePoints().count() <= 20;
  }
}
