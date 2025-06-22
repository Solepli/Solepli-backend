package com.ilta.solepli.domain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ilta.solepli.domain.auth.dto.request.BasicLoginRequest;
import com.ilta.solepli.domain.auth.dto.response.LoginResponse;
import com.ilta.solepli.domain.auth.entity.LoginType;
import com.ilta.solepli.domain.auth.service.oauth.OAuthService;
import com.ilta.solepli.domain.auth.service.oauth.OAuthServiceFactory;
import com.ilta.solepli.domain.solmark.place.entity.SolmarkPlaceCollection;
import com.ilta.solepli.domain.solmark.place.repository.SolmarkPlaceCollectionRepository;
import com.ilta.solepli.domain.user.entity.Role;
import com.ilta.solepli.domain.user.entity.User;
import com.ilta.solepli.domain.user.repository.UserRepository;
import com.ilta.solepli.domain.user.service.UserService;
import com.ilta.solepli.global.exception.CustomException;
import com.ilta.solepli.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final OAuthServiceFactory oauthServiceFactory;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final SolmarkPlaceCollectionRepository solmarkPlaceCollectionRepository;

  @Value("${DEFAULT_PROFILE_URL}")
  private String defaultImageUrl;

  @Transactional
  public void signup(BasicLoginRequest request) {
    String loginId = request.loginId();

    if (userRepository.existsByLoginId(loginId)) {
      throw new CustomException(ErrorCode.USER_EXISTS);
    }

    User savedUser =
        userRepository.save(
            User.builder()
                .role(Role.ADMIN)
                .loginId(loginId)
                .password(passwordEncoder.encode(request.password()))
                .profileImageUrl(defaultImageUrl)
                .nickname(userService.generateAdminNickname())
                .loginType(LoginType.BASIC)
                .build());

    SolmarkPlaceCollection solmarkPlaceCollection =
        SolmarkPlaceCollection.builder().name("저장 리스트").iconId(1).user(savedUser).build();

    solmarkPlaceCollectionRepository.save(solmarkPlaceCollection);
  }

  @Transactional(readOnly = true)
  public LoginResponse login(BasicLoginRequest request) {
    User user =
        userRepository
            .findByLoginId(request.loginId())
            .orElseThrow(() -> new CustomException(ErrorCode.INCORRECT_ACCOUNT));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new CustomException(ErrorCode.INCORRECT_ACCOUNT);
    }

    String accessToken = jwtTokenProvider.generateToken(user);

    return LoginResponse.from(accessToken, user.getRole());
  }

  @Transactional
  public LoginResponse socialLogin(String code, String input) {
    LoginType loginType;
    try {
      loginType = LoginType.valueOf(input);
    } catch (IllegalStateException e) {
      throw new CustomException(ErrorCode.INCORRECT_LOGIN_TYPE);
    }

    OAuthService oauthService = oauthServiceFactory.getOAuthService(loginType);

    String loginId = oauthService.getLoginId(oauthService.getAccessToken(code));

    User findUser = userService.findOrSignUpUser(loginId, loginType);

    String accessToken = jwtTokenProvider.generateToken(findUser);

    return LoginResponse.from(accessToken, findUser.getRole());
  }
}
