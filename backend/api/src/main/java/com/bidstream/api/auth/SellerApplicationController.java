package com.bidstream.api.auth;

import com.bidstream.application.auth.ApplySellerRoleUseCase;
import com.bidstream.application.auth.AuthTokens;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class SellerApplicationController {

  private final ApplySellerRoleUseCase applySellerRoleUseCase;

  public SellerApplicationController(ApplySellerRoleUseCase applySellerRoleUseCase) {
    this.applySellerRoleUseCase = applySellerRoleUseCase;
  }

  @PostMapping("/seller-application")
  public AuthResponse applySeller(Authentication authentication, HttpServletRequest request) {
    long userId = (long) authentication.getPrincipal();
    AuthTokens tokens = applySellerRoleUseCase.execute(userId, request.getHeader("User-Agent"));
    return AuthResponse.from(tokens);
  }
}
