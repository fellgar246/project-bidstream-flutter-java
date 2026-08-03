package com.bidstream.domain.lot;

import java.util.Optional;

public record LotQuery(
    Optional<LotStatus> status,
    Optional<Long> categoryId,
    Optional<Long> minPriceCents,
    Optional<Long> maxPriceCents,
    Optional<Long> sellerId,
    Optional<String> searchQuery,
    int page,
    int size,
    LotSort sort) {

  public LotQuery {
    if (page < 0) {
      throw new IllegalArgumentException("page must be >= 0");
    }
    if (size < 1 || size > 100) {
      throw new IllegalArgumentException("size must be between 1 and 100");
    }
  }
}
