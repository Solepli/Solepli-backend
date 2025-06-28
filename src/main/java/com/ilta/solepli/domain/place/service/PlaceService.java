package com.ilta.solepli.domain.place.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.place.dto.reqeust.RequestAddPlaceRequest;
import com.ilta.solepli.domain.place.dto.response.PlaceSearchResponse;
import com.ilta.solepli.domain.place.repository.PlaceRepository;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class PlaceService {

  @Value("${mail.to}")
  private String toEmail; // 수신 메일 설정

  @Value("${mail.from}")
  private String fromEmail; // 발신 메일 설정

  private static final String subject = "쏠플리 장소 추가 요청"; // 메일 제목

  private final PlaceRepository placeRepository;
  private final JavaMailSender javaMailSender;

  @Transactional(readOnly = true)
  public List<PlaceSearchResponse> getSearchPlaces(String keyword) {
    return placeRepository.getPlacesByKeyword(keyword);
  }

  public void requestAddPlace(CustomUserDetails customUserDetails, RequestAddPlaceRequest req) {
    try {
      // MimeMessage 객체 생성 및 초기화
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // 사용자 정보 및 현재 시간 추출
      User user = customUserDetails.user();
      String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
      // 응답으로 받은 영어의 카테고리를 한글로 변환
      String categories = changeCategoryToKo(req.category());

      // HTML 포맷의 메일 본문 생성
      String htmlContent =
          String.format(
              """
                                      <b>■ 장소명</b><br>
                                      %s<br><br>
                                      <b>■ 주소</b><br>
                                      %s<br><br>
                                      <b>■ 카테고리</b><br>
                                      %s<br><br>
                                      <b>■ 추가 메모</b><br>
                                      - %s<br><br>
                                      <b>■ 사용자 정보</b><br>
                                      - 사용자 닉네임: %s<br>
                                      - 전송 시각: %s<br>
                                      """,
              req.placeName(), req.address(), categories, req.note(), user.getNickname(), now);

      helper.setTo(toEmail); // 받는 사람
      helper.setFrom(fromEmail); // 보내는 사람
      helper.setSubject(subject); // 메일 제목
      helper.setText(htmlContent, true); // 메일 본문(HTML 형식)

      // 메일 발송
      javaMailSender.send(message);
    } catch (MessagingException e) {
      throw new CustomException(ErrorCode.MAIL_SEND_FAIL);
    }
  }

  private String changeCategoryToKo(List<String> categories) {
    Map<String, String> categoryMap =
        Map.of(
            "work", "공부/작업",
            "cafe", "카페",
            "walk", "산책/야외활동",
            "culture", "문화/예술",
            "shop", "쇼핑",
            "entertainment", "오락/여가",
            "food", "식당",
            "drink", "주점");

    return categories.stream()
        .map(categoryMap::get)
        .filter(Objects::nonNull) // 혹시 없는 값은 제외
        .collect(Collectors.joining(", "));
  }
}
