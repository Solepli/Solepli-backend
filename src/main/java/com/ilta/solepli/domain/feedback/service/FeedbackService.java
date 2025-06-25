package com.ilta.solepli.domain.feedback.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.ilta.solepli.domain.feedback.dto.AddFeedbackRequest;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.util.CustomUserDetails;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

  @Value("${spring.mail.username}")
  private String toEmail; // 수신 메일 설정

  @Value("${spring.mail.username}")
  private String fromEmail; // 발신 메일 설정

  private static final String subject = "쏠플리 사용자 의견"; // 메일 제목

  private final JavaMailSender javaMailSender;

  public void sendFeedbackEmail(CustomUserDetails customUserDetails, AddFeedbackRequest req) {
    try {
      // MimeMessage 객체 생성 및 초기화
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // 사용자 정보 및 현재 시간 추출
      User user = customUserDetails.user();
      String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

      // HTML 포맷의 메일 본문 생성
      String htmlContent =
          String.format(
              """
                              <b>■ 사용자 의견</b><br>
                              %s<br><br>
                              <b>■ 사용자 정보</b><br>
                              - 사용자 닉네임: %s<br>
                              - 전송 시각: %s
                              """,
              req.feedback(), user.getLoginId(), now);

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
}
