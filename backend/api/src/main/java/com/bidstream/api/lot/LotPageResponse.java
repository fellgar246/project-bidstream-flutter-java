package com.bidstream.api.lot;

import java.util.List;

public record LotPageResponse(List<LotResponse> content, PageMetadata page) {

  public record PageMetadata(int number, int size, long totalElements, int totalPages) {}
}
