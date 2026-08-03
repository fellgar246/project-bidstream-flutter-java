package com.bidstream.domain.auth;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {

  RefreshToken save(long userId, String tokenHash, Instant expiresAt, String userAgent);

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  void revoke(long id, Long replacedById);

  void revokeAllForUser(long userId);
}
