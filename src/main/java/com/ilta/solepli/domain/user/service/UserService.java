package com.ilta.solepli.domain.user.service;

import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.auth.entity.LoginType;
import com.ilta.solepli.domain.solmark.place.entity.SolmarkPlaceCollection;
import com.ilta.solepli.domain.solmark.place.repository.SolmarkPlaceCollectionRepository;
import com.ilta.solepli.domain.user.entity.Role;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final SolmarkPlaceCollectionRepository solmarkPlaceCollectionRepository;

  private final String[] ADJECTIVES = {
    "귀여운", "용감한", "날쌘", "온순한", "영리한", "수줍은", "호기심많은", "느긋한", "활발한", "우아한", "엉뚱한", "애교많은", "부지런한"
  };

  private final String[] NOUNS = {
    "고양이", "강아지", "여우", "호랑이", "토끼", "펭귄", "다람쥐", "늑대", "사슴", "고래", "햄스터", "코끼리", "기린", "판다"
  };

  @Value("${DEFAULT_PROFILE_URL}")
  private String defaultImageUrl;

  @Transactional(readOnly = true)
  public String generateAdminNickname() {
    String prefix = "관리자";

    Optional<User> latestAdmin =
        userRepository.findTopByNicknameStartingWithOrderByNicknameDesc(prefix);

    int nextNumber = 1;

    if (latestAdmin.isPresent()) {
      String latestNickname = latestAdmin.get().getNickname(); // 관리자27
      String numberPart = latestNickname.replace(prefix, ""); // 27

      try {
        nextNumber = Integer.parseInt(numberPart) + 1;
      } catch (NumberFormatException e) {
        // 만약 형식이 다르면 1로 시작
        nextNumber = 1;
      }
    }

    return prefix + nextNumber; // ex) 관리자28
  }

  @Transactional
  public User findOrSignUpUser(String loginId, LoginType loginType) {

    Random random = new Random();
    String nickname;

    do {
      String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
      String noun = NOUNS[random.nextInt(NOUNS.length)];
      int number = 100 + random.nextInt(900);
      nickname = adjective + noun + number;
    } while (checkNickname(nickname));

    final String checkedNickname = nickname;

    return userRepository
        .findByLoginId(loginId)
        .orElseGet(
            () -> {
              User savedUser =
                  userRepository.save(
                      User.builder()
                          .role(Role.USER)
                          .loginId(loginId)
                          .profileImageUrl(defaultImageUrl)
                          .nickname(checkedNickname)
                          .loginType(loginType)
                          .build());

              SolmarkPlaceCollection solmarkPlaceCollection =
                  SolmarkPlaceCollection.builder().name("저장 리스트").iconId(1).user(savedUser).build();

              solmarkPlaceCollectionRepository.save(solmarkPlaceCollection);

              return savedUser;
            });
  }

  @Transactional(readOnly = true)
  public boolean checkNickname(String nickname) {
    return userRepository.existsByNickname(nickname);
  }
}
