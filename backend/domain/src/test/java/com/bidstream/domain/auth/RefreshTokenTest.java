package com.bidstream.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  @Test
  void isRevoked_whenRevokedAtSet() {
    RefreshToken token =
        new RefreshToken(
            1L,
            1L,
            "hash",
            Instant.now().plusSeconds(60),
            Instant.now(),
            null,
            null,
            Instant.now());

    assertThat(token.isRevoked()).isTrue();
  }

  @Test
  void isExpired_whenPastExpiry() {
    RefreshToken token =
        new RefreshToken(
            1L, 1L, "hash", Instant.parse("2020-01-01T00:00:00Z"), null, null, null, Instant.now());

    assertThat(token.isExpired(Instant.parse("2026-01-01T00:00:00Z"))).isTrue();
  }
}
