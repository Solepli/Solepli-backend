package com.ilta.solepli.global.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.response.ErrorResponse;

public class ResponseUtil {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static void writeError(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getHttpStatus().value());
    response.setContentType("application/json;charset=UTF-8");

    ErrorResponse errorResponse =
        ErrorResponse.create()
            .httpStatus(errorCode.getHttpStatus())
            .message(errorCode.getMessage());

    objectMapper.writeValue(response.getWriter(), errorResponse);
  }
}
