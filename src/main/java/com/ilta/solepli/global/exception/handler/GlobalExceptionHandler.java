package com.ilta.solepli.global.exception.handler;

import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import lombok.extern.slf4j.Slf4j;

import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;
import com.ilta.solepli.global.exception.InvalidRequestException;
import com.ilta.solepli.global.response.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException ex) {

    log.error("handleHttpRequestMethodNotSupportedException", ex);

    final ErrorResponse response =
        ErrorResponse.create().httpStatus(HttpStatus.METHOD_NOT_ALLOWED).message(ex.getMessage());

    return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(value = {IllegalArgumentException.class})
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {

    log.error("handleIllegalArgumentException : {}", ex.getMessage());

    ErrorResponse response =
        ErrorResponse.create().message(ex.getMessage()).httpStatus(HttpStatus.BAD_REQUEST);

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(value = {NullPointerException.class})
  public ResponseEntity<ErrorResponse> handleNullPointException(NullPointerException ex) {

    log.error("handleNullPointException : {}", ex.getMessage());

    ex.printStackTrace();

    ErrorResponse response =
        ErrorResponse.create().message(ex.getMessage()).httpStatus(HttpStatus.BAD_REQUEST);

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(value = {HttpMessageNotReadableException.class})
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {

    log.error("HttpMessageNotReadableException : {}", ex.getMessage());

    String message = "요청 형식이 잘못되었습니다. (JSON 파싱 오류)";

    ErrorResponse response =
        ErrorResponse.create().message(message).httpStatus(HttpStatus.BAD_REQUEST);

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * @Vaild 검증 실패 시 에러 처리
   */
  @ExceptionHandler(value = {BindException.class})
  protected ResponseEntity<ErrorResponse> handleBindException(BindException ex) {

    log.error("handleBindException : {}", ex.getMessage());

    String message = ex.getMessage();

    String defaultMsg =
        message.substring(
            message.lastIndexOf("[") + 1, message.lastIndexOf("]") - 1); // "[" 또는 "]" 기준으로 메시지 추출
    log.error("에러 메세지 : {}", defaultMsg);

    ErrorResponse response =
        ErrorResponse.create().message(defaultMsg).httpStatus(HttpStatus.BAD_REQUEST);

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(value = {CustomException.class})
  protected ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {

    log.error("handleCustomException", ex);

    ErrorCode errorCode = ex.getErrorCode();
    String message = ex.getMessage();

    ErrorResponse response =
        ErrorResponse.create().message(message).httpStatus(errorCode.getHttpStatus());

    return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
  }

  @ExceptionHandler(value = {DateTimeParseException.class})
  protected ResponseEntity<ErrorResponse> handleDateTimeParseException(DateTimeParseException ex) {

    log.error("handleDateTimeParseException", ex);

    ErrorResponse response =
        ErrorResponse.create().message(ex.getMessage()).httpStatus(HttpStatus.BAD_REQUEST);

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(value = {InvalidRequestException.class})
  protected ResponseEntity<ErrorResponse> handleInvalidRequestException(
      InvalidRequestException ex) {

    log.error("handleInvalidRequestException : {}", ex.getMessage());

    ErrorResponse response =
        ErrorResponse.create().message(ex.getMessage()).httpStatus(HttpStatus.BAD_REQUEST);

    return ResponseEntity.badRequest().body(response);
  }

  /** 파일 용량 초과 (개별 파일 또는 전체 multipart 요청 크기 초과) */
  @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
  public ResponseEntity<?> handleMultipartException(Exception ex) {

    log.error("handleMultipartException : {}", ex.getMessage());

    String message = "파일 업로드 용량을 초과했습니다. 각 파일은 최대 5MB, 총 요청은 최대 500MB까지 허용됩니다.";

    ErrorResponse response =
        ErrorResponse.create().message(message).httpStatus(HttpStatus.BAD_REQUEST);

    return ResponseEntity.badRequest().body(response);
  }
}
