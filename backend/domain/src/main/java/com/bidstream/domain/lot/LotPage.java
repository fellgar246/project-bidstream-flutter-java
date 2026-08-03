package com.bidstream.domain.lot;

import java.util.List;

public record LotPage(List<Lot> content, int page, int size, long totalElements) {

  public int totalPages() {
    if (size == 0) {
      return 0;
    }
    return (int) Math.ceil((double) totalElements / size);
  }
}
