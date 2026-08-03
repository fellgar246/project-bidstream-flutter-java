package com.bidstream.application.lot;

import com.bidstream.application.cache.CachedLotPageSnapshot;
import com.bidstream.application.cache.CachedLotSnapshot;
import com.bidstream.application.cache.CachedLotSnapshotBuilder;
import com.bidstream.application.cache.LotReadCacheService;
import com.bidstream.domain.lot.LotQuery;
import com.bidstream.domain.lot.LotRepository;
import com.bidstream.domain.lot.LotSearchFacets;
import com.bidstream.domain.lot.LotSearchResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListLotsUseCase {

  private final LotService lotService;
  private final LotRepository lotRepository;
  private final LotReadCacheService lotReadCacheService;
  private final CachedLotSnapshotBuilder snapshotBuilder;

  public ListLotsUseCase(
      LotService lotService,
      LotRepository lotRepository,
      LotReadCacheService lotReadCacheService,
      CachedLotSnapshotBuilder snapshotBuilder) {
    this.lotService = lotService;
    this.lotRepository = lotRepository;
    this.lotReadCacheService = lotReadCacheService;
    this.snapshotBuilder = snapshotBuilder;
  }

  @Transactional(readOnly = true)
  public LotListResult execute(LotQuery query) {
    if (hasSearch(query)) {
      return fromSearch(lotRepository.search(query));
    }
    CachedLotPageSnapshot cached =
        lotReadCacheService.getLotPage(query, lotService::listPublicLots);
    Optional<LotSearchFacets> facets =
        query.facets() ? Optional.of(new LotSearchFacets(List.of(), List.of())) : Optional.empty();
    return new LotListResult(
        cached.content(), cached.page(), cached.size(), cached.totalElements(), facets);
  }

  private LotListResult fromSearch(LotSearchResult search) {
    List<CachedLotSnapshot> snapshots =
        search.page().content().stream()
            .map(lot -> snapshotBuilder.build(lot, search.ranksByLotId().get(lot.id())))
            .toList();
    return new LotListResult(
        snapshots,
        search.page().page(),
        search.page().size(),
        search.page().totalElements(),
        search.facets());
  }

  private static boolean hasSearch(LotQuery query) {
    return query.searchQuery().filter(q -> !q.isBlank()).isPresent();
  }
}
