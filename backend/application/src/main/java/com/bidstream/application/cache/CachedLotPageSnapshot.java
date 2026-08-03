package com.bidstream.application.cache;

import java.util.List;

public record CachedLotPageSnapshot(
    List<CachedLotSnapshot> content, int page, int size, long totalElements) {

  public int totalPages() {
    if (size == 0) {
      return 0;
    }
    return (int) Math.ceil((double) totalElements / size);
  }
}
