package com.bidstream.domain.auth;

import java.time.Instant;

public record RefreshToken(
    long id,
    long userId,
    String tokenHash,
    Instant expiresAt,
    Instant revokedAt,
    Long replacedById,
    String userAgent,
    Instant createdAt) {

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now);
  }
}
