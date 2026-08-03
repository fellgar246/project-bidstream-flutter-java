package com.bidstream.application.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplySellerRoleUseCase {

  private final AuthService authService;

  public ApplySellerRoleUseCase(AuthService authService) {
    this.authService = authService;
  }

  @PreAuthorize("hasRole('BUYER')")
  @Transactional
  public AuthTokens execute(long userId, String userAgent) {
    return authService.applySellerRole(userId, userAgent);
  }
}
