package com.bidstream.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {

  private final AuthService authService;

  public LogoutUseCase(AuthService authService) {
    this.authService = authService;
  }

  @Transactional
  public void execute(String refreshToken) {
    authService.logout(refreshToken);
  }
}
