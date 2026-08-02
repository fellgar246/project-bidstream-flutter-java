package com.bidstream.application.auth;

import com.bidstream.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

  private final AuthService authService;

  public RegisterUserUseCase(AuthService authService) {
    this.authService = authService;
  }

  @Transactional
  public User execute(String email, String password, String displayName) {
    return authService.register(email, password, displayName);
  }
}
