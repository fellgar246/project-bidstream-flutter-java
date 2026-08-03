package com.bidstream.api.bid;

import java.util.List;

public record BidPageResponse(List<BidSummary> content, PageMetadata page) {

  public record PageMetadata(int number, int size, long totalElements, int totalPages) {}
}
