package com.bidstream.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

  private final AuthService authService;

  public LoginUseCase(AuthService authService) {
    this.authService = authService;
  }

  @Transactional
  public AuthTokens execute(String email, String password, String userAgent) {
    return authService.login(email, password, userAgent);
  }
}
