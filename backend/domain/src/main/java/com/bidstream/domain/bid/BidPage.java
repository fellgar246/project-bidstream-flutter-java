package com.bidstream.domain.bid;

import java.util.List;

public record BidPage(List<Bid> content, int page, int size, long totalElements) {

  public int totalPages() {
    if (size <= 0) {
      return 0;
    }
    return (int) Math.ceil((double) totalElements / size);
  }
}
