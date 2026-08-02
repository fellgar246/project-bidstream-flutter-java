package com.bidstream.application.auth;

import com.bidstream.domain.auth.TokenReuseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshSessionUseCase {

  private final AuthService authService;

  public RefreshSessionUseCase(AuthService authService) {
    this.authService = authService;
  }

  @Transactional(noRollbackFor = TokenReuseException.class)
  public AuthTokens execute(String refreshToken, String userAgent) {
    return authService.refresh(refreshToken, userAgent);
  }
}
