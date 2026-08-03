package com.bidstream.domain.lot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LotImageTest {

  private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

  @Test
  void createPending_hasExpectedDefaults() {
    LotImage image =
        LotImage.createPending(1L, "lots/1/abc.jpg", 0, "image/jpeg", 1024L, NOW);

    assertThat(image.id()).isZero();
    assertThat(image.lotId()).isEqualTo(1L);
    assertThat(image.status()).isEqualTo(LotImageStatus.PENDING);
    assertThat(image.thumbnailKey()).isNull();
  }

  @Test
  void markReady_transitionsStatus() {
    LotImage pending =
        LotImage.createPending(1L, "lots/1/abc.jpg", 0, "image/jpeg", 1024L, NOW);
    Instant later = NOW.plusSeconds(60);

    LotImage ready = pending.markReady(later);

    assertThat(ready.status()).isEqualTo(LotImageStatus.READY);
    assertThat(ready.updatedAt()).isEqualTo(later);
  }

  @Test
  void markFailed_transitionsStatus() {
    LotImage pending =
        LotImage.createPending(1L, "lots/1/abc.jpg", 0, "image/jpeg", 1024L, NOW);

    LotImage failed = pending.markFailed(NOW.plusSeconds(30));

    assertThat(failed.status()).isEqualTo(LotImageStatus.FAILED);
  }
}
