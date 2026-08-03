package com.bidstream.api.lot;

import java.util.List;

public record LotPageResponse(
    List<LotResponse> content, PageMetadata page, LotFacetsResponse facets) {

  public LotPageResponse(List<LotResponse> content, PageMetadata page) {
    this(content, page, null);
  }

  public record PageMetadata(int number, int size, long totalElements, int totalPages) {}
}
