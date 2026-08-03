package com.bidstream.api.lot;

import java.util.List;

public record LotFacetsResponse(
    List<CategoryFacetResponse> categories, List<PriceRangeFacetResponse> priceRanges) {

  public record CategoryFacetResponse(long id, String name, long count) {}

  public record PriceRangeFacetResponse(long fromCents, long toCents, long count) {}
}
