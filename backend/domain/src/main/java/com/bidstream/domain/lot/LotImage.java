package com.bidstream.domain.lot;

import java.time.Instant;
import java.util.Objects;

public record LotImage(
    long id,
    long lotId,
    String storageKey,
    String thumbnailKey,
    int position,
    String contentType,
    long sizeBytes,
    LotImageStatus status,
    Instant createdAt,
    Instant updatedAt) {

  public LotImage {
    Objects.requireNonNull(storageKey, "storageKey");
    Objects.requireNonNull(contentType, "contentType");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
  }

  public static LotImage createPending(
      long lotId,
      String storageKey,
      int position,
      String contentType,
      long sizeBytes,
      Instant now) {
    return new LotImage(
        0L, lotId, storageKey, null, position, contentType, sizeBytes, LotImageStatus.PENDING, now, now);
  }

  public LotImage markReady(Instant now) {
    return new LotImage(
        id, lotId, storageKey, thumbnailKey, position, contentType, sizeBytes, LotImageStatus.READY, createdAt, now);
  }

  public LotImage markFailed(Instant now) {
    return new LotImage(
        id, lotId, storageKey, thumbnailKey, position, contentType, sizeBytes, LotImageStatus.FAILED, createdAt, now);
  }

  public LotImage withThumbnail(String thumbnailKey, Instant now) {
    return new LotImage(
        id, lotId, storageKey, thumbnailKey, position, contentType, sizeBytes, status, createdAt, now);
  }

  public LotImage withPosition(int newPosition, Instant now) {
    return new LotImage(
        id, lotId, storageKey, thumbnailKey, newPosition, contentType, sizeBytes, status, createdAt, now);
  }
}
