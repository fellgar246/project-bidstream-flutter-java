package com.bidstream.api.auth;

import com.bidstream.application.auth.AuthTokens;

public record AuthResponse(
    String accessToken, String refreshToken, long expiresIn, UserResponse user) {

  public static AuthResponse from(AuthTokens tokens) {
    return new AuthResponse(
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.expiresIn(),
        UserResponse.from(tokens.user()));
  }
}
