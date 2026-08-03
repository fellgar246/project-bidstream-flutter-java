package com.bidstream.domain.lot;

import java.util.List;

public record LotSearchFacets(List<CategoryFacet> categories, List<PriceRangeFacet> priceRanges) {}
