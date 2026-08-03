package com.bidstream.application.auth.port;

import com.bidstream.domain.user.User;
import java.util.List;
import java.util.Optional;

public interface JwtPort {

  String createAccessToken(User user);

  Optional<AccessTokenClaims> parseAccessToken(String token);

  record AccessTokenClaims(long userId, String email, List<String> roles, String jti) {}
}
