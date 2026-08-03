package com.bidstream.application.lot;

import com.bidstream.domain.lot.LotSearchFacets;
import java.util.List;
import java.util.Optional;

public record LotListResult(
    List<com.bidstream.application.cache.CachedLotSnapshot> content,
    int page,
    int size,
    long totalElements,
    Optional<LotSearchFacets> facets) {

  public int totalPages() {
    if (size == 0) {
      return 0;
    }
    return (int) Math.ceil((double) totalElements / size);
  }
}
