package com.bidstream.domain.lot;

import java.util.Map;
import java.util.Optional;

public record LotSearchResult(
    LotPage page, Map<Long, Double> ranksByLotId, Optional<LotSearchFacets> facets) {}
